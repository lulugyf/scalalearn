#coding=utf-8

#需要支持jdbc.url的轮替分配

import sys
import os

# for BLE
#================= for fuqing
config_ble = (
    ({"host_begin":86, "host_end":91, "dir_begin":1, "dir_end":3},
                '''"10.113.181.{host}-{dir}":{brl}
  "netty_listen_hostname": "10.113.181.{host}",
  "netty_listen_port": "{host}{dir}1",
  "ble_id_seq":"{bleseq}",
  "jdbc_url": "{jdbcurl}",
  "zookeeper_addr":"{zookeeperaddr}",
  "jmx_jolokiaPort": "{host}{dir}2"{brr},''',

 {"brl":"{", "brr":"}", "zookeeperaddr":"10.113.161.103:8671,10.113.161.104:8671,10.113.161.105:8671,10.105.92.50:8671,10.105.92.51:8671",
        "bleseq":"20000001,20000002,20000003,20000004,20000005,20000006,20000007,20000008,20000009,20000010,20000011,20000012,20000013",
  },
  ("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.120)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.122)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.124)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))",
   "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.122)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.124)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.120)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))",
   "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.124)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.120)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.122)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))"
    ),
              ),

# ================for xiqu
            ( {"host_begin":96, "host_end":101, "dir_begin":1, "dir_end":3},
 '''"10.113.182.{host}-{dir}":{brl}
  "netty_listen_hostname": "10.113.182.{host}",
  "netty_listen_port": "{host}{dir}1",
  "ble_id_seq":"{bleseq}",
  "jdbc_url": "{jdbcurl}",
  "zookeeper_addr":"{zookeeperaddr}",
  "jmx_jolokiaPort": "{host}{dir}2"
  {brr},''',

 {"brl":"{", "brr":"}", "zookeeperaddr":"10.113.172.56:8671,10.113.172.57:8671,10.113.172.58:8671,10.112.185.2:8671,10.112.185.3:8671",
        "bleseq":"10000001,10000002,10000003,10000004,10000005,10000006,10000007,10000008,10000009,10000010,10000011,10000012,10000013",
  },
              (
      "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.140)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.144)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.142)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))",
      "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.142)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.140)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.144)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))",
      "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.144)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.142)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.140)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))"
              ),
    )

            )

def ble():
    fo = sys.stdout
    for region in config_ble: # 分机房
        #host_begin, host_end, dir_begin, dir_end = region[0]
        r = region[0]
        ss = region[1]
        parm = region[2]
        jdbcurls = region[3]
        for host in range(r['host_begin'], r['host_end']+1):  # 分主机
            for pathid in range(r['dir_begin'], r['dir_end']+1):  # 分目录
                parm['host'] = host
                parm['dir'] = pathid
                parm['jdbcurl'] = jdbcurls[pathid % len(jdbcurls)]
                fo.write((ss+"\n").format(**parm))
        parm['host'] = r['host_begin']; parm['dir'] = "4"; parm['jdbcurl'] = jdbcurls[0]
        fo.write((ss+"\n").format(**parm))


#  for Broker
config_broker = (
#================= for fuqing
 ({"host_begin":86, "host_end":91, "dir_begin":1, "dir_end":3},
                '''"10.113.181.{host}-{dir}":{brl}
  "netty_listen_hostname": "10.113.181.{host}",
  "netty_listen_port": "{host}{dir}3",
  "netty_http_listen_port":"{host}{dir}4",
  "jdbc_url": "{jdbcurl}",
  "zookeeper_addr":"{zookeeperaddr}",
  "jmx_jolokiaPort": "{host}{dir}5"
  {brr},''',

 {"brl":"{", "brr":"}", "zookeeperaddr":"10.113.161.103:8671,10.113.161.104:8671,10.113.161.105:8671,10.105.92.50:8671,10.105.92.51:8671" },
  (
   "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.120)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.122)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.124)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))",
   "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.122)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.124)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.120)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))",
   "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.124)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.120)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.122)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))"
    ),
              ),

# ================for xiqu
            ( {"host_begin":96, "host_end":101, "dir_begin":1, "dir_end":3},
 '''"10.113.182.{host}-{dir}":{brl}
  "netty_listen_hostname": "10.113.182.{host}",
  "netty_listen_port": "{host}{dir}3",
  "netty_http_listen_port":"{host}{dir}4",
  "jdbc_url": "{jdbcurl}",
  "zookeeper_addr":"{zookeeperaddr}",
  "jmx_jolokiaPort": "{host}{dir}5"
  {brr},''',

 {"brl":"{", "brr":"}", "zookeeperaddr":"10.113.172.56:8671,10.113.172.57:8671,10.113.172.58:8671,10.112.185.2:8671,10.112.185.3:8671"  },
              (
      "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.140)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.144)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.142)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))",
      "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.142)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.140)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.144)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))",
      "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.144)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.142)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.140)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))"
              ),
    )
)


def broker():
    fo = sys.stdout
    for region in config_broker: # 分机房
        #host_begin, host_end, dir_begin, dir_end = region[0]
        r = region[0]
        ss = region[1]
        parm = region[2]
        jdbcurls = region[3]
        for host in range(r['host_begin'], r['host_end']+1):  # 分主机
            for pathid in range(r['dir_begin'], r['dir_end']+1):  # 分目录
                parm['host'] = host
                parm['dir'] = pathid
                parm['jdbcurl'] = jdbcurls[pathid % len(jdbcurls)]
                fo.write((ss+"\n").format(**parm))
        fo.write((ss + "\n").format(host=r['host_begin'], dir="4", jdbcurl=jdbcurls[0]))

hostmap = '''
10.113.182.96:11101,10.113.181.86
10.113.182.96:11102,10.113.181.87
10.113.182.96:11103,10.113.181.88
10.113.182.96:11104,10.113.181.89
10.113.182.96:11105,10.113.181.90
10.113.182.96:11106,10.113.181.91
10.113.182.96:21101,10.113.182.96
10.113.182.96:21102,10.113.182.97
10.113.182.96:21103,10.113.182.98
10.113.182.96:21104,10.113.182.99
10.113.182.96:21105,10.113.182.100
10.113.182.96:21106,10.113.182.101
'''
hostlist = (
    '10.113.181.86','10.113.181.87','10.113.181.88','10.113.181.89','10.113.181.90','10.113.181.91',
    '10.113.182.96','10.113.182.97','10.113.182.98','10.113.182.99','10.113.182.100','10.113.182.101',
)
props = """{host}-{i},{hostaddr},idmm,/tmp/idmm3/idmm-broker{i},resource/ssh_Identity"""
# 生成 部署位置列表
def deploy_pos():
    fo = sys.stdout
    hosts = {l[l.find(',')+1:] : l[:l.find(',')] for l in hostmap.strip().split("\n")}
    for host in hostlist:
        for i in range(1, 4):
            fo.write((props+"\n").format(host=host, hostaddr=hosts[host], i=i))


# 生成要同步的配置文件列表, 后边的0 表示最后修改时间, 同步程序会根据修改时间决定哪些文件
def __dir_walk(fo, path, files):
    for f in files:
        if f.endswith('.proplist'): continue
        fpath = "%s/%s" % (path, f)
        if os.path.isdir(fpath): continue
        st = os.stat(fpath)
        fo.write("%s/%s %d999\n" % (path, f, st.st_mtime))
    #print param, path, files
def deploy_files():
    import os
    for path in ("config", "lib"):
        os.path.walk(path, __dir_walk, sys.stdout)


def files():
    fs = { line.strip(): 1 for line in open("f")}
    fo = open("deploy_files.txt.tmp", "w")
    for line in open("deploy_files.txt"):
        f = line.strip().split()
        if len(f) < 2: continue
        if fs.get(f[0], None) is not None:
            fo.write("%s 0\n" % f[0])
        else:
            fo.write(line)
    fo.close()
    os.remove("deploy_files.txt")
    os.rename("deploy_files.txt.tmp", "deploy_files.txt")

# python tool.py ble > config/ble/server-ble-oracle.properties.proplist
# python tool.py broker > config/broker/server-oracle.properties.proplist
# python tool.py deploy >deploy_pos.txt
# python tool.py files >deploy_files.txt
if __name__ == '__main__':
    if len(sys.argv) > 1:
        ctype = sys.argv[1]
        if ctype == "ble":
            print("{")
            ble()
            print("}")
        elif ctype == "broker":
            print("{")
            broker()
            print("}")
        elif ctype == "pos":
            deploy_pos()
        elif ctype == "files":
            deploy_files()
        elif ctype == 'ff':
            files()
