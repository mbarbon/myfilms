#!/bin/sh

if test "$1" == ""; then
    echo "Usage: $0 [trovacinema|filmup]"
    exit 1
fi

cat <<EOF | grep "^$1" |
trovacinema_results	http://trovacinema.repubblica.it/programmazione-cinema/citta/firenze/fi/film
trovacinema_details	http://trovacinema.repubblica.it/film/bar-sport/406159
filmup_results		http://filmup.leonardo.it/cgi-bin/search.cgi?ps=10&fmt=long&q=underworld&ul=%25%2Fsc_%25&x=0&y=0&m=all&wf=0020&wm=wrd&sy=0
filmup_card		http://filmup.leonardo.it/sc_underworld.htm
filmup_review		http://filmup.leonardo.it/underworld.htm
EOF

while read name url; do
    wget -O res/raw/$name $url
done
