import pandas as pd

if __name__ == '__main__':

    df = pd.read_csv('test.csv', header=None)
    response_time = df.iloc[:, 1:]
    response_time.transpose().describe().transpose()