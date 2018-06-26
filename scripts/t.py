import os

os.system("mvn dependency:copy-dependencies -DoutputDirectory=lib")

cp = ["target/classes"]
d = "lib"
for f in os.listdir(d):
    if not f.endswith(".jar"): continue
    cp.append(d + "/" + f)

fo = open("setcp.cmd", "w")
fo.write("set CLASSPATH=")
fo.write(";".join(cp))
fo.close()
