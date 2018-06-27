dp=`dirname $0`

cd $dp

cp="scalalearn-0.1.jar"
for f in lib/*.jar
do
cp="$cp:$f"
done

export CLASSPATH=$cp

date

~/jdk1.8.0_131/bin/java gyf.test.scala.CheckDBAndKillProc app_xq.properties