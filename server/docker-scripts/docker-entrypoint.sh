#!/bin/bash
systemctl start postgresql-14
systemctl enable sshd.service
systemctl start sshd.service
su -c "startServer" - "genesis-symphony"
echo "Logged as genesis-symphony, starting server"
tail -f /dev/null