#!/bin/sh -e

git add .
git commit -a -m "Html pages updated"
git push origin gh-pages

echo
echo
echo
echo "Commit done."
echo "   http://pcingola.github.com/BigDataScript/"
