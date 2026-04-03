#!/usr/bin/env python3
"""
独立训练脚本
在独立进程中运行训练，完全隔离资源
"""
import argparse
import json
import logging
import sys
import os
from pathlib import Path

# 添加项目路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

import pandas as pd
from training.arima_trainer import ARIMATrainer
from utils.data_processor import DataProcessor

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


def set_low_priority():
    """设置低优先级运行（跨平台兼容）"""
    try:
        import psutil
        process = psutil.Process()
        
        # 设置CPU亲和性（使用较少的核心）
        try:
            cpu_count = psutil.cpu_count()
            max_cpus = max(1, cpu_count // 2)  # 只使用一半的CPU核心
            cpus_to_use = list(range(max_cpus))
            process.cpu_affinity(cpus_to_use)
            logger.info(f"设置CPU亲和性: 使用核心 {cpus_to_use}")
        except Exception as e:
            logger.warning(f"设置CPU亲和性失败: {e}")
        
        # 设置进程优先级（使用psutil的跨平台API）
        try:
            # Windows和Linux/Mac使用不同的API
            if sys.platform == 'win32':
                # Windows: 使用优先级类常量
                # psutil 在 Windows 上 nice() 返回的是优先级类名称字符串
                try:
                    # 尝试设置为 BELOW_NORMAL_PRIORITY_CLASS
                    # 注意：psutil 的 Windows 版本可能不支持直接设置 nice
                    # 使用 IDLE_PRIORITY_CLASS (4) 或 BELOW_NORMAL_PRIORITY_CLASS (16384)
                    import psutil._pswindows as pswin
                    process.nice(pswin.IDLE_PRIORITY_CLASS)
                    logger.info("设置进程优先级: IDLE")
                except AttributeError:
                    # 如果无法访问 _pswindows，跳过设置
                    logger.warning("Windows 优先级设置不可用")
            else:
                # Linux/Mac: 使用 nice 值
                if hasattr(process, 'nice'):
                    process.nice(10)
                    logger.info("设置进程优先级: nice=10")
        except Exception as e:
            logger.warning(f"设置进程优先级失败: {e}")
            
    except Exception as e:
        logger.warning(f"设置低优先级失败: {e}")


def train_model(data_file: str, output_dir: str) -> dict:
    """
    训练ARIMA模型
    
    Args:
        data_file: 数据文件路径
        output_dir: 模型输出目录
        
    Returns:
        训练结果
    """
    try:
        # 设置低优先级
        set_low_priority()
        
        logger.info("=" * 60)
        logger.info("开始独立训练进程")
        logger.info("=" * 60)
        
        # 加载数据
        logger.info(f"加载数据: {data_file}")
        data_path = Path(data_file)
        if not data_path.exists():
            raise FileNotFoundError(f"数据文件不存在: {data_file}")
        
        # 读取数据
        df = pd.read_csv(data_file)
        
        if 'Date' in df.columns and 'Passengers' in df.columns:
            # 日度数据
            df['Date'] = pd.to_datetime(df['Date'])
            df = df.sort_values('Date').reset_index(drop=True)
            passenger_series = df['Passengers']
            logger.info(f"已加载日度数据: {len(passenger_series)} 条记录")
        elif 'Month' in df.columns and 'Passengers' in df.columns:
            # 月度数据，需要转换
            processor = DataProcessor()
            monthly_data = processor.load_monthly_data(data_file)
            daily_data = processor.monthly_to_daily(
                interpolation_method='cubic',
                apply_effects=True,
                apply_perturbation=True,
                noise_level=0.15,
                noise_type='adaptive',
                random_seed=42
            )
            passenger_series = daily_data['Passengers']
            logger.info(f"已转换月度数据为日度数据: {len(passenger_series)} 条记录")
        else:
            raise ValueError(f"数据文件格式错误，需要包含Date/Month和Passengers列")
        
        # 创建训练器
        trainer = ARIMATrainer(p=5, d=1, q=0)
        
        # 寻找最优参数
        logger.info("寻找最优ARIMA参数...")
        trainer.find_best_params(
            passenger_series,
            method='aic',
            use_parallel=False,  # 在独立进程中不使用并行，避免资源竞争
            max_p=3,  # 减少参数搜索空间
            max_d=2,
            max_q=3
        )
        
        # 训练模型
        logger.info(f"训练ARIMA({trainer.p},{trainer.d},{trainer.q})模型...")
        history = trainer.train(
            passenger_series,
            validate=True,
            test_size=30
        )
        
        # 保存模型
        logger.info(f"保存模型到: {output_dir}")
        os.makedirs(output_dir, exist_ok=True)
        trainer.save_model(output_dir)
        
        # 输出结果
        result = {
            'success': True,
            'message': '训练完成',
            'model_info': {
                'order': [trainer.p, trainer.d, trainer.q],
                'aic': history.get('aic'),
                'bic': history.get('bic'),
                'validation': history.get('validation')
            }
        }
        
        logger.info("=" * 60)
        logger.info("训练完成")
        logger.info(f"模型参数: ARIMA({trainer.p},{trainer.d},{trainer.q})")
        logger.info(f"AIC: {history.get('aic', 'N/A')}")
        logger.info(f"BIC: {history.get('bic', 'N/A')}")
        logger.info("=" * 60)
        
        return result
        
    except Exception as e:
        logger.error(f"训练失败: {e}", exc_info=True)
        return {
            'success': False,
            'message': '训练失败',
            'error': str(e)
        }


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='独立ARIMA模型训练')
    parser.add_argument('--data', type=str, required=True, help='训练数据文件路径')
    parser.add_argument('--output', type=str, required=True, help='模型输出目录')
    
    args = parser.parse_args()
    
    # 执行训练
    result = train_model(args.data, args.output)
    
    # 输出JSON结果
    print(json.dumps(result, indent=2))
    
    # 返回退出码
    sys.exit(0 if result['success'] else 1)


if __name__ == '__main__':
    main()
