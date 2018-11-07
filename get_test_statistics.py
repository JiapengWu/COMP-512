import pandas as pd
import sys

if __name__ == '__main__':
    with open(sys.argv[1], "r") as f:
        lines = f.readlines()
    largest = max(list(map(lambda x: len(x), lines)))
    df = pd.read_csv(sys.argv[1], names=list(range(largest)), header=None)
    ndf = df.iloc[:, 0]
    ndf.columns = ['load']
    response_time = df.iloc[:, 1:]
    result = response_time.transpose().describe().transpose()
    result = pd.concat([ndf, result], axis=1)
    result.to_csv("{}_stats.csv".format(sys.argv[1].split(".")[0]))
    print(result)