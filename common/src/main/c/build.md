Here are the build guidelines for the C part of tenet project, you need to install clang and llvm.

For Windows (Visual studio needs to be installed):

building executable : `clang -fcolor-diagnostics -fansi-escape-codes -g -O2 -o lib_win.exe -v wepoll.c lib_win.c`

building shared : `clang -fcolor-diagnostics -fansi-escape-codes -g -shared -O2 -o lib_win.dll -v wepoll.c lib_win.c`




For Linux :

building executable : `clang -fcolor-diagnostics -fansi-escape-codes -g -O2 -o lib_linux -v lib_linux.c`

building shared : `clang -fcolor-diagnostics -fansi-escape-codes -g -shared -fPIC -O2 -o lib_linux.so -v lib_linux.c`




For Macos :

building executable : `clang -fcolor-diagnostics -fansi-escape-codes -g -O2 -o lib_macos -v lib_macos.c`

building shared : `clang -fcolor-diagnostics -fansi-escape-codes -g -shared -fPIC -O2 -o lib_macos.dylib -v lib_macos.c`


Then copy the shared library under /resources/lib to use them.

Method start with w means implemented in windows.
Method start with l means implemented in linux.
Method start with m means implemented in macos.
Method start with g means implemented in all platform.
