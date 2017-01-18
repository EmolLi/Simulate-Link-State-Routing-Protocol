# Simulate-Link-State-Routing-Protocol

## Compile the program

```
$ mvn compile
```

### Router commands

```
$ attach [Process IP] [Process Port] [Link Weight] //establishes new link to the other routers
$ start //starts the router
$ connect [Process IP] [Process Port] [IP address] [Link Weight]
$ disconnect [Port Number] //remove the link
$ detect [IP Address] //outputs path from source to destination
$ neighbors //outputs the ip address of all neighbors of the router
$ quit
```



