$PREFIX/local/bin/proot --kill-on-exit -w $PWD -b /dev:/dev -b /proc:/proc -b /system:/system -b /vendor:/vendor -b /storage:/storage -b /sdcard:/sdcard -b /apex:/apex -b /dev/urandom:/dev/random -b /proc/self/fd:/dev/fd -b /proc/self/fd/0:/dev/stdin -b /proc/self/fd/1:/dev/stdout -b /proc/self/fd/2:/dev/stderr -b $PREFIX:$PREFIX -0 -l -r $PREFIX/local/alpine sh $PREFIX/local/bin/init "$@"