#!/bin/sh

rm -rf target

model=maven.mdo
repoLocal=`egrep '^maven.repo.local=' $HOME/build.properties | sed 's/maven.repo.local=//'`
packageWithVersion=false
dir=target/generated-sources
model_version=3.0.0
classesDir=target/classes

echo Repository: $repoLocal

mkdir -p $dir

if $cygwin; then
  repoLocal=`cygpath -pu "$repoLocal"`
fi

CP=$repoLocal/modello/jars/modello-1.0-SNAPSHOT.jar:$repoLocal/xstream/jars/xstream-1.0-SNAPSHOT.jar:$repoLocal/xpp3/jars/xpp3-1.1.3.3.jar

if $cygwin; then
  CP=`cygpath -pw "$CP"`
fi

java -classpath "$CP" org.codehaus.modello.Modello $model java "$dir" "$model_version" "$package_with_version"
ret=$?; if [ $ret != 0 ]; then exit $ret; fi
java -classpath "$CP" org.codehaus.modello.Modello $model xpp3 "$dir" "$model_version" "$package_with_version"
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

# TODO: remove after I can fix modello
patch target/generated-sources/org/apache/maven/model/io/xpp3/MavenXpp3Writer.java <properties-patch.diff
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

# TODO: remove after can specify package name to modello
mv target/generated-sources/org/apache/maven/model target/generated-sources/org/apache/maven/project
for i in `find target/generated-sources -name '*.java' -type f`
do
  echo Repackaging $i...
  cat $i | sed 's/org.apache.maven.model/org.apache.maven.project/g' >tmp
  mv tmp $i
done

mkdir -p $classesDir
javac -classpath $CP -d $classesDir -sourcepath $dir `find . -name '*.java' -type f`
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

(
  cd $classesDir
  jar cf ../maven-model-1.1-SNAPSHOT.jar *
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

cp target/maven-model-1.1-SNAPSHOT.jar $repoLocal/maven/jars
