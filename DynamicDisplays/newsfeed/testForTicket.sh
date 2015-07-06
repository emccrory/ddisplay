#!/bin/bash

if /usr/krb5/bin/klist > /tmp/k ; then 
    echo yes there is a kerberos ticket for me; 
else 
    echo no there is NOT a kerberos ticket for me; 
fi

cat /tmp/k