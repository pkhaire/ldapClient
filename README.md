# ldapClient - Simple LDAP user and group lookup tool

ldapClient is a simple one-class LDAP lookup tool with the ability to search objects (users, groups, etc.) and display attributes.

It is written in Java and built with Gradle with the option of generating a native image with GraalVM.


## Usage

Executing `ldapClient -h` or `ldapClient --help` will print the usage information:

```
usage: LdapClient [-a] [-B <arg>] [-g <arg>] [-G <arg>] [-H <arg>] [-h]
       [-P <arg>] [-u <arg>] [-U <arg>] [-v]
Simple LDAP user and group lookup tool.
 -a,--printAttributes     Print attributes of all users and groups found
 -B,--bindingDN <arg>     DN of binding user, defaults to 'cn=Directory
                          Manager'
 -g,--lookupGroup <arg>   Name of group to lookup, defaults to '*'
 -G,--groupQuery <arg>    LDAP group query, defaults to:
                          '(&(cn={0})(objectClass=groupOfUniqueNames))'
 -H,--ldapUrl <arg>       URL of the LDAP server, defaults to
                          'ldap://localhost:1389'
 -h,--help                Usage doc
 -P,--bindingPW <arg>     Password of binding user
 -u,--lookupUser <arg>    Name of user to lookup, defaults to '*'
 -U,--userQuery <arg>     LDAP user query, defaults to:
                          '(&(uid={0})(objectClass=inetOrgPerson))'
 -v,--verbose             Debug traces
```

Some usage examples:
```
$ ./ldapClient -H ldap://localhost:1389 -B "cn=Directory Manager" -P password -g "*" -G "(&(cn={0})(objectClass=groupOfUniqueNames))" -u "*" -U "(&(uid={0})(objectClass=inetOrgPerson))"

Looking up users with filter: (&(uid=*)(objectClass=inetOrgPerson))
Number of users found : 152
Looking up groups with filter: (&(cn=*)(objectClass=groupOfUniqueNames))
Number of groups found : 5
```

```
./ldapClient -H ldap://localhost:1389 -B "cn=Directory Manager" -P password -g "HR Managers" -G "(&(cn={0})(objectClass=groupOfUniqueNames))" -u "nbohr" -U "(&(uid={0})(objectClass=inetOrgPerson))" -a

Looking up users with filter: (&(uid=nbohr)(objectClass=inetOrgPerson))
Number of users found : 1
  DN = uid=nbohr,ou=People,dc=example,dc=com
    givenName = Niels
    sn = Bohr
    telephoneNumber = +1 408 555 1212
    userPassword = [B@7fce3ae11828
    l = San Francisco
    ou = People
    uidNumber = 1111
    gidNumber = 1000
    roomNumber = 0007
    mail = nbohr@example.com
    uid = nbohr
    facsimileTelephoneNumber = +1 408 555 1213
    objectClass = top
    cn = Niels Bohr
    homeDirectory = /home/nbohr
Looking up groups with filter: (&(cn=HR Managers)(objectClass=groupOfUniqueNames))
Number of groups found : 1
  DN = cn=HR Managers,ou=groups,dc=example,dc=com
    ou = groups
    description = People who can manage HR entries
    objectClass = top
    uniqueMember = uid=kvaughan, ou=People, dc=example,dc=com
    cn = HR Managers
```


## Developer notes

The Gradle build uses these plugins:

* `java`
* `application`

The GraalVM integration for native image generation is delegated to this plugin:

* `com.palantir.graal`

The only external dependency is Apache `commons-cli`.

For LDAP conectivity the following provider is used:
`com.sun.jndi.ldap.LdapCtxFactory`.


## Integration tests

For testing the client application we can use a simple pre-configured LDAP like [OpenDJ](https://hub.docker.com/r/openidentityplatform/opendj/):

```
docker run -h ldap-01.domain.com -p 1389:1389 -p 1636:1636 -p 4444:4444 --name ldap-01 openidentityplatform/opendj
```

Test data is available at the Github repository as ldif file [Example.ldif](https://github.com/OpenIdentityPlatform/OpenDJ/blob/master/src/site/resources/Example.ldif).

The import is not automated right now. If you want to preserve the import you can map a persistant volume:

```
docker run -h ldap-01.domain.com -e BASE_DN='dc=example,dc=com' -v /your_folder/data:/opt/opendj/data -p 1389:1389 -p 1636:1636 -p 4444:4444 --name ldap-01 openidentityplatform/opendj
```


### Testing on CentOS

In order to to test the GraalVM native image on CentOS 7 a [Vagrantfile](./Vagrantfile) and an Ansible [playbook](./ansible/ldapClient-playbook.yml) is used.

Manual execution may be done by calling:
```
ansible-playbook -v -i .vagrant/provisioners/ansible/inventory/vagrant_ansible_inventory ansible/ldapClient-playbook.yml
```


## TODO

* Usage of `ldaps` protocol with native image
  - See https://github.com/oracle/graal/issues/1074
  

