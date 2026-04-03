"""
压力测试脚本
测试训练任务与接口服务同时运行时的性能表现
"""
import asyncio
import aiohttp
import time
import statistics
from typing import List, Dict
from datetime import datetime
import concurrent.futures


class StressTest:
    """压力测试类"""

    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.results: List[Dict] = []

    async def test_api_response_time(
        self,
        endpoint: str,
        method: str = "GET",
        payload: Dict = None,
        iterations: int = 100
    ) -> Dict:
        """测试API响应时间"""
        durations = []
        errors = 0

        async with aiohttp.ClientSession() as session:
            for i in range(iterations):
                start = time.time()
                try:
                    if method == "GET":
                        async with session.get(f"{self.base_url}{endpoint}") as resp:
                            await resp.text()
                            status = resp.status
                    else:
                        async with session.post(
                            f"{self.base_url}{endpoint}",
                            json=payload
                        ) as resp:
                            await resp.text()
                            status = resp.status

                    if status >= 500:
                        errors += 1

                except Exception as e:
                    errors += 1
                    print(f"请求失败: {e}")

                duration = (time.time() - start) * 1000  # ms
                durations.append(duration)

                # 每10个请求打印一次进度
                if (i + 1) % 10 == 0:
                    print(f"进度: {i + 1}/{iterations}")

        return {
            'endpoint': endpoint,
            'iterations': iterations,
            'avg_response_time': statistics.mean(durations),
            'p95_response_time': self._percentile(durations, 95),
            'p99_response_time': self._percentile(durations, 99),
            'min_response_time': min(durations),
            'max_response_time': max(durations),
            'error_rate': errors / iterations,
            'timestamp': datetime.now().isoformat()
        }

    def _percentile(self, data: List[float], percentile: int) -> float:
        """计算百分位数"""
        sorted_data = sorted(data)
        index = int(len(sorted_data) * percentile / 100)
        return sorted_data[min(index, len(sorted_data) - 1)]

    async def test_concurrent_requests(
        self,
        endpoint: str,
        concurrency: int = 10,
        total_requests: int = 100
    ) -> Dict:
        """测试并发请求性能"""
        semaphore = asyncio.Semaphore(concurrency)
        durations = []
        errors = 0

        async def make_request():
            nonlocal errors
            async with semaphore:
                start = time.time()
                try:
                    async with aiohttp.ClientSession() as session:
                        async with session.get(f"{self.base_url}{endpoint}") as resp:
                            await resp.text()
                            if resp.status >= 500:
                                errors += 1
                except Exception as e:
                    errors += 1
                    print(f"并发请求失败: {e}")

                duration = (time.time() - start) * 1000
                durations.append(duration)

        # 创建所有任务
        tasks = [make_request() for _ in range(total_requests)]
        await asyncio.gather(*tasks)

        return {
            'endpoint': endpoint,
            'concurrency': concurrency,
            'total_requests': total_requests,
            'avg_response_time': statistics.mean(durations),
            'p95_response_time': self._percentile(durations, 95),
            'p99_response_time': self._percentile(durations, 99),
            'error_rate': errors / total_requests,
            'timestamp': datetime.now().isoformat()
        }

    async def test_with_training(
        self,
        training_duration: int = 60,
        check_interval: int = 5
    ) -> Dict:
        """
        在训练期间测试接口性能

        Args:
            training_duration: 训练持续时间（秒）
            check_interval: 检查间隔（秒）
        """
        results = []

        # 启动训练任务
        print("启动训练任务...")
        async with aiohttp.ClientSession() as session:
            async with session.post(
                f"{self.base_url}/api/mgmt/v1/prediction/train/async"
            ) as resp:
                data = await resp.json()
                task_id = data.get('data', {}).get('task_id')
                print(f"训练任务ID: {task_id}")

        # 在训练期间持续测试接口
        start_time = time.time()
        iteration = 0

        while time.time() - start_time < training_duration:
            iteration += 1
            print(f"\n=== 第 {iteration} 轮测试 ===")

            # 测试模型信息接口
            result = await self.test_api_response_time(
                "/api/mgmt/v1/prediction/model-info",
                iterations=10
            )
            results.append(result)
            print(f"模型信息接口 - 平均响应: {result['avg_response_time']:.2f}ms, "
                  f"P95: {result['p95_response_time']:.2f}ms")

            # 测试健康检查接口
            result = await self.test_api_response_time(
                "/api/mgmt/v1/prediction/health",
                iterations=10
            )
            results.append(result)
            print(f"健康检查接口 - 平均响应: {result['avg_response_time']:.2f}ms, "
                  f"P95: {result['p95_response_time']:.2f}ms")

            # 等待下一次检查
            await asyncio.sleep(check_interval)

        # 汇总结果
        all_avg_times = [r['avg_response_time'] for r in results]
        all_p95_times = [r['p95_response_time'] for r in results]

        return {
            'test_duration': training_duration,
            'total_checks': iteration,
            'overall_avg_response': statistics.mean(all_avg_times),
            'overall_p95_response': statistics.mean(all_p95_times),
            'max_avg_response': max(all_avg_times),
            'max_p95_response': max(all_p95_times),
            'results': results
        }

    def print_report(self, result: Dict):
        """打印测试报告"""
        print("\n" + "=" * 60)
        print("压力测试报告")
        print("=" * 60)

        for key, value in result.items():
            if key != 'results':
                if isinstance(value, float):
                    print(f"{key}: {value:.2f}")
                else:
                    print(f"{key}: {value}")

        print("=" * 60)


async def main():
    """主函数"""
    tester = StressTest()

    print("开始压力测试...")

    # 测试1: 基础API响应时间（无训练）
    print("\n### 测试1: 基础API响应时间 ###")
    result = await tester.test_api_response_time(
        "/api/mgmt/v1/prediction/model-info",
        iterations=50
    )
    tester.print_report(result)

    # 测试2: 并发请求测试
    print("\n### 测试2: 并发请求测试 ###")
    result = await tester.test_concurrent_requests(
        "/api/mgmt/v1/prediction/model-info",
        concurrency=20,
        total_requests=100
    )
    tester.print_report(result)

    # 测试3: 训练期间的接口性能
    print("\n### 测试3: 训练期间接口性能 ###")
    result = await tester.test_with_training(
        training_duration=60,
        check_interval=5
    )
    tester.print_report(result)

    print("\n压力测试完成!")


if __name__ == "__main__":
    asyncio.run(main())
