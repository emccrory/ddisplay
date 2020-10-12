#!/bin/sh

LOCAL=$(git  rev-parse  "@{0}")
REMOTE=$(git rev-parse  "master@{u}")
ORIGIN=$(git ls-remote origin -h refs/heads/master | awk '{ print $1 }')
BASE=$(git   merge-base "@{0}" "master@{u}")

# echo $LOCAL
# echo $ORIGIN
# echo $REMOTE
# echo $BASE

if [ "$LOCAL" = "$ORIGIN" ]; then
    if [ "$LOCAL" = "$REMOTE" ]; then
	echo "Local and orgin are synced";
    elif [ "$LOCAL" = "$BASE" ]; then
	echo "Local and origin are synced, but need to pull from local base"
    elif [ "$REMOTE" = "$BASE" ]; then
	echo "Local and origin are synced, but need to push to local base"
    else
	echo "Local has diverged, but in sync with origin"
    fi
else
    if [ "$LOCAL" = "$REMOTE" ]; then
	echo "Local and orgin are out of sync";
    elif [ "$LOCAL" = "$BASE" ]; then
	echo "Need to pull from local base and then sync with origin"
    elif [ "$REMOTE" = "$BASE" ]; then
	echo "Need to push to local base and then sync with origin"
    else
	echo "Local has diverged"
    fi
fi

