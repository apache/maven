
toc=`pwd`/index.apt

#
# Top matter
#

echo " ------ " > $toc
echo " Summary of Maven 2.x documentation " >> $toc
echo " ------ " >> $toc
echo " Jason van Zyl " >> $toc
echo " ------ " >> $toc
echo " 12 October 2005 " >> $toc
echo " ------ " >> $toc
echo >> $toc
echo "Documentation" >> $toc
echo >> $toc
echo "* Getting Started Guide" >> $toc
echo >> $toc
echo " * {{{getting-started/index.html}Getting Started Guide}}" >> $toc

#
# Mini Guides
#

echo >> $toc
echo "* Mini Guides" >> $toc
echo >> $toc

(
  cd mini

  for i in `ls -d guide-*`
  do
   if [ -d $i ]
   then
     (
       cd $i
       title=`grep "^ *Guide" ${i}.apt | sed 's/^ *//'`
       i=`echo $i | sed 's/\.apt/\.html/'`
       [ ! -z "$title" ] && echo " * {{{mini/$i/$i.html}$title}}" >> $toc && echo >> $toc
     )
   else
     title=`grep "^ Guide" $i | sed 's/^ *//'`
     i=`echo $i | sed 's/\.apt/\.html/'`
     [ ! -z "$title" ] && echo " * {{{mini/$i}$title}}" >> $toc && echo >> $toc
   fi
  
  done       
)       

#
# Introductions
#

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

#
# Plugins
#

echo >> $toc
echo "* Plugin Guides" >> $toc
echo >> $toc

(
  cd plugin

  for i in `ls guide-*.apt`
  do
   title=`grep "^ Guide" $i | sed 's/^ *//'`
   i=`echo $i | sed 's/\.apt/\.html/'`
   [ ! -z "$title" ] && echo " * {{{plugin/$i}$title}}" >> $toc && echo >> $toc
  done       
)       

#
# Developer Guides
#

echo "" >> $toc
echo "* Developer Guides " >> $toc
echo "" >> $toc
echo " * {{{development/guide-m2-development.html}Guide to Developing Maven 2.x}}" >> $toc
echo " " >> $toc
echo " * {{{development/guide-building-m2.html}Guide to Building Maven 2.x}}" >> $toc
