import pandas as pd

if __name__ == '__main__':

    df = pd.read_csv('test.csv', header=None)
    ndf = df.iloc[:, 0]
    ndf.columns = ['load']
    response_time = df.iloc[:, 1:]
    result = response_time.transpose().describe().transpose()
    result = pd.concat([ndf, result], axis=1)
    print(result)