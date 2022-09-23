rm $(find ./bin/ -maxdepth 1 -type f)
echo rm files: $?
rm ./bin/.settings/*
echo rm .settings: $?
rm -d ./bin/.settings/
echo rm .settings: $?
