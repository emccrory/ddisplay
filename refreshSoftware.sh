#!/bin/bash

cd ~/src

if [ -e roc-dynamicdisplays-old5 ] ; then
    rm -rf roc-dynamicdisplays-old5;
fi
if [ -e roc-dynamicdisplays-old4 ] ; then
    mv roc-dynamicdisplays-old4 roc-dynamicdisplays-old5;
fi
if [ -e roc-dynamicdisplays-old3 ] ; then
    mv roc-dynamicdisplays-old3 roc-dynamicdisplays-old4;
fi
if [ -e roc-dynamicdisplays-old2 ] ; then
    mv roc-dynamicdisplays-old2 roc-dynamicdisplays-old3;
fi
if [ -e roc-dynamicdisplays-old1 ] ; then
    mv roc-dynamicdisplays-old1 roc-dynamicdisplays-old2;
fi
if [ -e roc-dynamicdisplays ] ; then
    mv roc-dynamicdisplays roc-dynamicdisplays-old1;
fi

git clone http://cdcvs.fnal.gov/projects/roc-dynamicdisplays

date

