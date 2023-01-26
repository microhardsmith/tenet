import subprocess
import sys

# 查找系统中是否存在clang
def searchClang():
    print("Searching for clang... ")
    process = subprocess.Popen('clang --version', shell = True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    lines = []
    for line in process.stdout.readlines() + process.stderr.readlines():
        lines.append(str(line, encoding='utf-8'))
    if lines[0].find('clang version') == 0:
        print('Clang installed')
        return lines[1]
    else:
        print('Clang not found in path, please install clang from "https://clang.llvm.org/" ')
        sys.exit()

# 在Windows系统下运行
def runningInWindows():
    print('Running in Windows')

# 在Linux系统下运行
def runningInLinux():
    print('Running in Linux')

# 在MacOS系统下运行
def runningInMacos():
    print('Running in macOS')



if __name__ == '__main__':
    targetOs = searchClang()
    if targetOs.find('windows') != -1:
        #windows系统下
        runningInWindows()
    elif targetOs.find('linux') != -1:
        #linux系统下
        runningInLinux()
    elif targetOs.find('apple') != -1:
        #mac系统下    
        runningInMacos()
    else :
        print('No supported system found')
        sys.exit()
    