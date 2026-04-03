"""
独立训练进程管理器
使用完全独立的子进程运行训练任务，彻底隔离资源
"""
import os
import sys
import json
import asyncio
import logging
import subprocess
from typing import Dict, Optional, Callable, Any
from pathlib import Path
from dataclasses import dataclass
from datetime import datetime

logger = logging.getLogger(__name__)


@dataclass
class TrainingResult:
    """训练结果"""
    success: bool
    message: str
    model_info: Optional[Dict] = None
    error: Optional[str] = None


class IndependentTrainingProcess:
    """
    独立训练进程管理器
    使用subprocess启动完全独立的Python进程运行训练
    """

    def __init__(self):
        self._current_process: Optional[subprocess.Popen] = None
        self._progress_callback: Optional[Callable] = None

    async def start_training(
        self,
        data_file: str,
        model_output_dir: str,
        progress_callback: Optional[Callable] = None
    ) -> TrainingResult:
        """
        启动独立训练进程

        Args:
            data_file: 数据文件路径
            model_output_dir: 模型输出目录
            progress_callback: 进度回调函数

        Returns:
            训练结果
        """
        self._progress_callback = progress_callback

        # 构建训练脚本路径
        script_path = Path(__file__).parent.parent.parent / "training" / "standalone_trainer.py"

        if not script_path.exists():
            return TrainingResult(
                success=False,
                message="训练脚本不存在",
                error=f"Script not found: {script_path}"
            )

        # 构建命令行参数
        cmd = [
            sys.executable,
            str(script_path),
            "--data", data_file,
            "--output", model_output_dir
        ]

        # 启动独立进程
        try:
            logger.info(f"启动独立训练进程: {' '.join(cmd)}")

            # 设置环境变量，确保UTF-8编码
            env = os.environ.copy()
            env['PYTHONIOENCODING'] = 'utf-8'

            self._current_process = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                # 不使用text=True，手动解码以避免编码问题
                env=env
            )

            # 异步等待进程完成
            return await self._monitor_process()

        except Exception as e:
            logger.error(f"启动训练进程失败: {e}")
            return TrainingResult(
                success=False,
                message="启动训练进程失败",
                error=str(e)
            )

    async def _monitor_process(self) -> TrainingResult:
        """监控训练进程"""
        if not self._current_process:
            return TrainingResult(success=False, message="进程未启动")

        try:
            # 异步读取输出
            stdout_lines = []
            stderr_lines = []

            # 创建异步读取任务
            async def read_stream(stream, lines, is_stderr=False):
                """异步读取流，使用UTF-8解码，并实时输出日志"""
                while True:
                    try:
                        # 读取字节数据
                        data = await asyncio.get_event_loop().run_in_executor(
                            None, stream.readline
                        )
                        if not data:
                            break
                        # 使用UTF-8解码，忽略错误
                        line = data.decode('utf-8', errors='ignore').strip()
                        if line:
                            lines.append(line)
                            # 实时输出训练日志
                            if is_stderr:
                                logger.warning(f"[训练进程] {line}")
                            else:
                                logger.info(f"[训练进程] {line}")
                            # 解析进度信息
                            if not is_stderr:
                                self._parse_progress(line)
                    except Exception as e:
                        logger.warning(f"读取流时出错: {e}")
                        break

            # 同时读取stdout和stderr
            await asyncio.gather(
                read_stream(self._current_process.stdout, stdout_lines, False),
                read_stream(self._current_process.stderr, stderr_lines, True)
            )

            # 等待进程完成
            return_code = await asyncio.get_event_loop().run_in_executor(
                None, self._current_process.wait
            )

            if return_code == 0:
                # 尝试从stdout解析JSON结果
                model_info = {}
                if stdout_lines:
                    # 尝试从后向前查找完整的JSON行
                    json_found = False
                    for line in reversed(stdout_lines):
                        line = line.strip()
                        if not line:
                            continue
                        # 尝试解析JSON
                        try:
                            # 检查是否以 { 开头（JSON对象）
                            if line.startswith('{'):
                                model_info = json.loads(line)
                                logger.info(f"解析训练结果成功: {model_info}")
                                json_found = True
                                break
                        except json.JSONDecodeError:
                            continue

                    if not json_found:
                        # 如果没有找到JSON，尝试合并多行
                        try:
                            # 尝试从最后几行合并（JSON可能被分割）
                            combined = ""
                            for line in reversed(stdout_lines[-10:]):  # 检查最后10行
                                line = line.strip()
                                if line:
                                    combined = line + combined
                                    try:
                                        model_info = json.loads(combined)
                                        logger.info(f"合并多行后解析训练结果成功: {model_info}")
                                        json_found = True
                                        break
                                    except json.JSONDecodeError:
                                        continue
                        except Exception as e:
                            logger.warning(f"合并解析JSON失败: {e}")

                    if not json_found:
                        logger.warning(f"无法解析训练结果JSON，stdout最后几行: {stdout_lines[-5:]}")
                        model_info = {"output": stdout_lines}

                return TrainingResult(
                    success=True,
                    message="训练完成",
                    model_info=model_info
                )
            else:
                return TrainingResult(
                    success=False,
                    message="训练失败",
                    error="\n".join(stderr_lines) if stderr_lines else "Unknown error"
                )

        except Exception as e:
            logger.error(f"监控训练进程异常: {e}")
            return TrainingResult(
                success=False,
                message="监控训练进程异常",
                error=str(e)
            )
        finally:
            self._current_process = None

    def _parse_progress(self, line: str):
        """解析进度信息"""
        # 这里可以解析训练脚本输出的进度信息
        if self._progress_callback and "progress" in line.lower():
            try:
                # 尝试解析JSON格式的进度
                progress_data = json.loads(line)
                self._progress_callback(progress_data)
            except:
                pass

    def terminate(self):
        """终止训练进程"""
        if self._current_process and self._current_process.poll() is None:
            self._current_process.terminate()
            logger.info("训练进程已终止")


# 全局实例
independent_trainer = IndependentTrainingProcess()
