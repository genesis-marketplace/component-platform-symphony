#!/bin/bash
source /home/genesis-symphony/.bashrc
systemctl start postgresql-14
su -c "source /home/genesis-symphony/.bashrc ; genesisInstall" - "genesis-symphony"
echo "genesisInstall done"