#!/bin/bash
export PATH="$PATH:$(pwd)"
UPLOAD="$UPLOAD_DIR/$(ls $UPLOAD_DIR | tr ' ' '\n' | grep -v "all")"

if ! test -f "$UPLOAD"; then
	echo "File does not exist. $UPLOAD"
	exit
fi

MOD=$(curl $API_ENDPOINT/api/client/servers/$SERVER_ID/files/list\?directory\=/mods -H "Authorization: Bearer $TOKEN" | jq -r ".data[] | .attributes.name" | grep "sfcraft")
echo "Fetch: $MOD"
if [ -n "$MOD" ]; then
	echo "Update target: $MOD"
	curl "$API_ENDPOINT/api/client/servers/$SERVER_ID/files/delete" -H "Content-Type: application/json" -H "Accept: application/json" -X POST -H "Authorization: Bearer $TOKEN" -d "{\"root\": \"/mods\", \"files\": [\"$MOD\"]}"
	echo "Deleted."
fi

echo "Uploading $UPLOAD"
SIGNED_URL=$(curl $API_ENDPOINT/api/client/servers/$SERVER_ID/files/upload -H "Authorization: Bearer $TOKEN" | jq -r ".attributes.url")

if [ -z "$SIGNED_URL" ]; then
	echo "!!! CANNOT FETCH SIGNED URL !!!"
	exit
fi

SIGNED_URL=$(echo $SIGNED_URL | sed "s_http://192.168.1.75:32303_$API_ENDPOINT/api-1_")

curl "$SIGNED_URL&directory=/mods" -X POST -F "files=@$UPLOAD"
echo "Uploaded!"

BROADCAST="Server is shutting down in 10s, please be ready."
UPDATE_CONTENT_COMMAND="tellraw @a {\"text\":$(echo $COMMIT_AUTHOR: $COMMIT_MESSAGE | jq -Ra .),\"color\":\"aqua\"}"
UPDATE_CONTENT_COMMAND=$(echo $UPDATE_CONTENT_COMMAND | jq -Ra .)
echo $UPDATE_CONTENT_COMMAND
curl "$API_ENDPOINT/api/client/servers/$SERVER_ID/command" -H "Content-Type: application/json" -H "Accept: application/json" -H "Authorization: Bearer $TOKEN" -X POST -d '{"command":"tellraw @a [{\"text\":\"< SFCRAFT UPDATE >\\n\",\"color\":\"aqua\"}]"}'
curl "$API_ENDPOINT/api/client/servers/$SERVER_ID/command" -H "Content-Type: application/json" -H "Accept: application/json" -H "Authorization: Bearer $TOKEN" -X POST -d "{\"command\":$UPDATE_CONTENT_COMMAND}"
sleep 1s
curl "$API_ENDPOINT/api/client/servers/$SERVER_ID/command" -H "Content-Type: application/json" -H "Accept: application/json" -H "Authorization: Bearer $TOKEN" -X POST -d "{\"command\":\"say $BROADCAST\"}"
echo "Broadcast is sent!"
sleep 10s

curl "$API_ENDPOINT/api/client/servers/$SERVER_ID/power" -H "Content-Type: application/json" -H "Accept: application/json" -H "Authorization: Bearer $TOKEN" -X POST -d "{\"signal\":\"stop\"}"
echo "STOP is sent!"

sleep 60s
curl "$API_ENDPOINT/api/client/servers/$SERVER_ID/power" -H "Content-Type: application/json" -H "Accept: application/json" -H "Authorization: Bearer $TOKEN" -X POST -d "{\"signal\":\"kill\"}"
echo "KILL is sent!"

sleep 15s
curl "$API_ENDPOINT/api/client/servers/$SERVER_ID/power" -H "Content-Type: application/json" -H "Accept: application/json" -H "Authorization: Bearer $TOKEN" -X POST -d "{\"signal\":\"start\"}"
echo "START IS SENT"
