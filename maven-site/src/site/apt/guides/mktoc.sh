
toc=`pwd`/index.apt

cp header.apt $toc

echo "* Mini Guides" >> $toc
echo >> $toc

(
  cd mini

  for i in `ls guide-*.apt`
  do
   title=`grep "^ Guide" $i | sed 's/^ *//'`
   i=`echo $i | sed 's/\.apt/\.html/'`
   [ ! -z "$title" ] && echo " * {{{mini/$i}$title}}" >> $toc && echo >> $toc
  done       
)       

echo >> $toc
echo "* Introductory Material" >> $toc
echo >> $toc

(
  cd introduction

  for i in `ls introduction-*.apt`
  do
   title=`grep "^ Introduction" $i | sed 's/^ *//'`
   i=`echo $i | sed 's/\.apt/\.html/'`   
   [ ! -z "$title" ] && echo " * {{{introduction/$i}$title}}" >> $toc && echo >> $toc
  done       
)       
