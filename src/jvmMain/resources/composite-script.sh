#!/bin/sh
(
  cat <<'EOF'
#!/bin/bash
echo "line 1"
invali $%&
sleep 1
echo "line 4"
EOF
) >script.sh
if [ -f "script.sh" ]; then
  chmod 755 "./script.sh"
  "./script.sh"
  wait
  rm script.sh
else
  echo "Error creating \"script.sh\""
fi

(
  cat <<'EOF'
#!/bin/sh
sleep 1
find .. -type d
echo "line 3"
for i in 1 2 3 4 5
do
  echo "Looping … number $i"
done
EOF
) >script.sh
if [ -f "script.sh" ]; then
  source "./script.sh"
  wait
  rm script.sh
else
  echo "Error creating \"script.sh\""
fi
