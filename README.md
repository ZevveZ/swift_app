# swift_app

## 已经实现的功能

本软件实现了android手机间基于Wi-Fi Direct的聊天和发送文件的功能，暂时没有与PC的版本。目前只能够支持两部手机进行聊天和发送文件。
1. 采用Wi-Fi Direct技术使得两部手机不需要开启热点，而仅仅需要开启Wi-Fi就可以进行连接，跟蓝牙类似，但是却比蓝牙提供的有效范围更广，速度也更加快。当然缺点就是会消耗更加多的电量。至于安全性方面，通过几个方面来保证：一是Wi-Fi覆盖的范围有限，二是类似于蓝牙，你只有在软件中开启对他人可见，别人才能够发现你的手机，并且你一旦手动断开与他人的连接后，对他人就不可见了，三是对他人可见的是有一定时间范围的，也是跟蓝牙类似。
2. 多线程的设计：聊天和收发文件使用的是两套socket、两个线程，收发文件时不会干扰到聊天。具体来说聊天采用的是一般的Thread。而收发文件则是使用AsyncTask，提供的函数可以很方便地处理下载文件的耗时操作和更新主线程的进度，但是由于对Notification的不熟悉，在软件中并没有加入通知栏显示下载进度的功能。AsyncTask可以很方便的管理线程，但是在Thread方面确实是遇到了困难，解决的详细过程会在作业的另一篇文本中阐述。
3. UI界面的设计：整个程序只有一个Activity，但是加入了两个侧滑的Fragment。具体的实现方式是使用了DrawerLayout，难度不大。两个Fragment分别用于显示文件列表和管理可见的用户列表。

## 已知的问题
1. 程序退出后文件能够正常继续下载，但是聊天就会终止，因为聊天功能所占用的线程和资源都能被正确地释放掉，但是此时再进入应用程序，如果作为聊天服务器的手机首先进入，那么能够正常地聊天，不需要再进行连接的操作(当然前提是基于Wi-Fi Direct的连接没有被断开，例如在距离过远、手动关闭连接或者一方关闭了Wi-Fi的情况下连接会断开)，如果作为客户端的手机先进入程序，则由于还没有服务器，建立不了连接
