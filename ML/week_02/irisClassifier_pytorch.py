# -*- coding: utf-8 -*-
"""week2part2.ipynb

Automatically generated by Colaboratory.

Original file is located at
    https://colab.research.google.com/drive/1lI6IWvhqEAP7s1EjFLFhj5CMh9tqRSVN
"""

import torch
import torch.nn as nn
import torch.nn.functional as F
from sklearn import datasets
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from torch.utils.data import TensorDataset, DataLoader
from google.colab import files
from sklearn.model_selection import train_test_split

iris = datasets.load_iris()

features = {'sepal_length':iris.data[:,2], 'sepal_width':iris.data[:,1], 'target':iris.target==0}

df = pd.DataFrame(data = features)
X = df.drop('target',axis =1).values
y = df.target.values

features = X
target = y

iris= TensorDataset(torch.FloatTensor(features),torch.LongTensor(target))

iris_loader = DataLoader(iris,batch_size=50,shuffle = True)

class Model(nn.Module):
    
    def __init__(self,in_features=2,h1=6,h2=3,out_features=3):
        super().__init__()
        self.fc1 = nn.Linear(in_features,h1)
        self.fc2 = nn.Linear(h1,h2)
        self.out = nn.Linear(h2,out_features)
    
    def forward(self,x):
        x = F.relu(self.fc1(x))
        x = F.relu(self.fc2(x))
        x = self.out(x)
        return x


model = Model()

features = torch.FloatTensor(features)
target = torch.LongTensor(target)

X_train, X_test, y_train, y_test=train_test_split(features,target,random_state=59,test_size=0.35)


# Classification problem => Evaluated based on Cross Entropy Loss
criterion = nn.CrossEntropyLoss()

#Optimizer
optimizer = torch.optim.Adam(model.parameters(),lr=0.01)

#Epochs
epochs = 200
losses = []

for i in range(epochs):
    
    ypred = model.forward(X_train)
    
    loss = criterion(ypred,y_train)
    #Keeping track of loss
    losses.append(loss.item())
    

    if i%10==0:
        print(f'Epoch:{i},loss:{loss:.2f}')
        
    #Backpropagation
    optimizer.zero_grad()
    loss.backward()
    optimizer.step()

plt.plot(range(epochs),losses)
plt.show()