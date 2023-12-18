import sys
from functools import reduce

import scipy.stats as stats
import json


def detect_results_format(filename):
    with open(filename, 'r') as f:
        try:
            data = json.load(f)
            if isinstance(data, dict):
                if "context" in data and "benchmarks" in data:
                    # format used by both google-benchmark and androidx-microbenchmark
                    return "google"
            elif isinstance(data, list):
                first = data[0]
                if isinstance(first, dict) and "jmhVersion" in first:
                    return "jmh"

            return "unknown"
        except json.JSONDecodeError:
            return "unknown"


class Benchmark:
    def __init__(self, name):
        self.name = name
        self.params = []
        self.metric = ""
        self.runs = []


def parse_jmh(filename):
    benchmarks = []
    with open(filename, 'r') as f:
        data = json.load(f)
        for result in data:
            bm = Benchmark(result['benchmark'])
            bm.metric = result['mode'] + ',' + result['primaryMetric']['scoreUnit']
            bm.params = result['params']
            bm.runs = reduce(lambda x, y: x + y, result['primaryMetric']['rawData'])
            benchmarks.append(bm)

    return benchmarks


def parse_google(filename, metric="timeNs"):
    benchmarks = []
    with open(filename, 'r') as f:
        data = json.load(f)
        for result in data['benchmarks']:
            methodName = result['name']
            params = {}
            if '[' in methodName:
                parts = methodName.split('[', 2)
                methodName = parts[0]
                params['parameters'] = parts[1][0:-1]

            name = result['className'] + "." + methodName
            bm = Benchmark(name)
            bm.params = params
            bm.metric = metric
            bm.runs = result['metrics'][metric]['runs']
            benchmarks.append(bm)

    return benchmarks


class ComparisonResult:
    def __init__(self, a, b):
        self.a = a
        self.b = b
        self.a_mean = 0
        self.a_ci = []
        self.b_mean = 0
        self.b_ci = []
        self.speedup = 0

    def to_string(self):
        return ("|" + str(self.a.params) + "|" + self.a.name + ("|%3f" % self.a_mean) +
                ("|±%.3f" % (self.a_ci[1] - self.a_mean)) +
                "|" + self.b.name + ("|%.3f" % self.b_mean) +
                ("|±%.3f" % (self.b_ci[1] - self.b_mean)) + (("|%.3f" % self.speedup) if self.speedup else "|N/A") + "|")


def compare_benchmarks(a, b, method='overlap', ci_level=0.999):
    if method != 'overlap':
        raise ArithmeticError("Unsupported method")
    if a.metric != b.metric:
        raise AssertionError("Metric mismatch")
    cmp = ComparisonResult(a, b)
    cmp.a_mean = stats.tmean(a.runs)
    cmp.a_ci = ci(a.runs, ci_level)
    cmp.b_mean = stats.tmean(b.runs)
    cmp.b_ci = ci(b.runs, ci_level)

    if cmp.a_ci[1] < cmp.b_ci[0] or cmp.b_ci[1] < cmp.a_ci[0]:
        # intervals don't overlap - that's great!
        cmp.speedup = cmp.b_mean / cmp.a_mean
    else:
        cmp.speedup = None
    return cmp


def ci(sample, level=0.999):
    N = len(sample)
    mean = stats.tmean(sample)
    sem = stats.sem(sample)
    return stats.t.interval(confidence=level, df=N - 1, loc=mean, scale=sem)


if __name__ == "__main__":
    filename = sys.argv[1]
    benchmark_a = sys.argv[2]
    benchmark_b = sys.argv[3]

    file_type = detect_results_format(filename)
    benchmarks = []
    if file_type == "jmh":
        benchmarks = parse_jmh(filename)
    elif file_type == "google":
        benchmarks = parse_google(filename, "timeNs")

    for bm in benchmarks:
        print(bm.name)

    a_instances = list(filter(lambda bm: bm.name == benchmark_a, benchmarks))
    b_instances = list(filter(lambda bm: bm.name == benchmark_b, benchmarks))

    for a in a_instances:
        b = list(filter(lambda bm: bm.params == a.params, b_instances))[0]
        cmp_res = compare_benchmarks(a, b)
        print(cmp_res.to_string())
