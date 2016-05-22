# BattleBablle
клон Агарио. 
Проект.
По умолчанию логирование в log4j.properties отключено, поскольку снижает производительность.


Сборка в консоле, в папке проекта:
    clean assemble build copyRuntimeLibs copyStartFile
или так, если нужны все библиотеки упаковаными в одном файле.
    clean fatJar copyStartFile

Запуск из
 папка проекта/build/libs
 runClient запуск сервера, runClient.cmd запуск клиента
