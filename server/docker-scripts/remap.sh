#!/bin/bash
source /home/genesis-symphony/.bashrc
systemctl start postgresql-14
su -c "source /home/genesis-symphony/.bashrc ; yes | remap --commit" - "genesis-symphony"
su -c "JvmRun global.genesis.environment.scripts.SendTable -t USER -f /home/genesis-symphony/run/site-specific/data/user.csv" - "genesis-symphony"
echo "remap done"
