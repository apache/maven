#!/bin/sh

toc=`pwd`/toc.apt

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

echo >> $toc
echo "* Guides" >> $toc
echo >> $toc
echo " * {{{getting-started/index.html}Getting Started Guide}}" >> $toc
echo >> $toc
echo " * {{{plugin-development/index.html}Plug-in Developer's Guide}}" >> $toc
echo >> $toc

echo "* Mini Guides" >> $toc
echo >> $toc

(
  cd mini

  for i in `ls guide-*.apt`
  do
   title=`grep "^ Guide" $i | sed 's/^ *//'`
   [ ! -z "$title" ] && echo " * {{{$i}$title}}" >> $toc && echo >> $toc
  done       
)       

echo >> $toc
echo "* Introductions" >> $toc
echo >> $toc

(
  cd introduction

  for i in `ls introduction-*.apt`
  do
   title=`grep "^ Introduction" $i | sed 's/^ *//'`
   [ ! -z "$title" ] && echo " * {{{$i}$title}}" >> $toc && echo >> $toc
  done       
)       
       
