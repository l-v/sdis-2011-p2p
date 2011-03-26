NAME
     MP2P - Multicast Peer to Peer client/server

SYNOPSIS
     java MP2P [-p Path] [-i IP] [-c CONTROLPORT] [-d DATAPORT] [-h HashType]

DESCRIPTION
	A peer to peer client/server that uses MULTICAST to share files in LANs.

OPTIONS
     -p Path
		Path to the folder with the shared files. Downloaded files go in this folder.

	-i IP
		Multicast group IP address. Defaults to 224.0.2.10.
	
	-c CONTROLPORT
		Multicast control port. Defaults to 8967.
	
	-d DATAPORT
		Multicast data port. Defaults to 8966.
	
	-h HashType
		Hash type used for the file ID. Defaults to SHA-256

PATH
	./files
		Folder with files to be shared. Downloaded files are also saved to this folder.
		Can be overridden with -p.

COMPILING
	There is a makefile for the project so to build it you just have to:
	$> make
	in the project directory.
	
AUTHORS
    Liliana Borges Vilela <ei08137@fe.up.pt>
	Ricardo Jorge de Sousa Teixeira <ei08040@fe.up.pt>



