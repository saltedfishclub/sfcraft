#!/bin/bash

UPLOAD=$(ls $UPLOAD | tr ' ' '\n' | grep -v "all")

if ! test -f "$UPLOAD"; then
	echo "File does not exist. $UPLOAD"
	exit
fi

MOD=$(special_curl $API_ENDPOINT/api/client/servers/$SERVER_ID/files/list\?directory\=/mods -H "Authorization: Bearer $TOKEN" | jq -r ".data[] | .attributes.name" | grep "sfcraft")
echo "Fetch: $MOD"
if [ -n "$MOD" ]; then
	echo "Update target: $MOD"
	special_curl "$API_ENDPOINT/api/client/servers/$SERVER_ID/files/delete" -H "Content-Type: application/json" -H "Accept: application/json" -X POST -H "Authorization: Bearer $TOKEN" -d "{\"root\": \"/mods\", \"files\": [\"$MOD\"]}"
	echo "Deleted."
fi

echo "Uploading $UPLOAD"
SIGNED_URL=$(special_curl $API_ENDPOINT/api/client/servers/$SERVER_ID/files/upload -H "Authorization: Bearer $TOKEN" | jq -r ".attributes.url")

if [ -z "$SIGNED_URL" ]; then
	echo "!!! CANNOT FETCH SIGNED URL !!!"
	exit
fi

special_curl "$SIGNED_URL&directory=/mods" -X POST -F "files=@$UPLOAD"
echo "Uploaded!"

