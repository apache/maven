How to use mboot
----------------

o export MBOOT_HOME=<whatever>
o export PATH=$PATH:$MBOOT_HOME
o ./build
o cd target
o ./mboot-install.sh
o mboot

mboot will build Maven projects that are relatively simple. It will build
a JAR and package any resources into the JAR. Good enough for Maven itself
and the plugins but appears to be useful for other things.
