Peer Group Manager README.txt
-----------------------------

Installation
------------

 * The PeerGroupManager library is provided as an Eclipse project.
   You can import it by using File->Import in Eclipse (note: when
   importing, make sure the "Copy projects into workspace" option is
   not checked).

 * Before you can successfully build the library project, you need to ensure 
   that it can resolve its dependencies on files from the AllJoyn SDK. To
   do this, you should:

   - Download and install the AllJoyn Android SDK version 2.3.3 or higher from
     http://www.alljoyn.org

   - Define a workspace variable named ALLJOYN_HOME using the following procedure:
     - In Eclipse, open Window->Preferences->General->Workspace->Linked Resources
     - Make sure "Enable linked resources" is checked
     - Define a new path variable named ALLJOYN_HOME, and set its location to the
       top level folder where you installed the downloaded AllJoyn Android SDK.
       For example, if your installed AllJoyn SDK folder structure were

         C:\alljoyn-3.0.2-android-sdk-dbg\
           alljoyn_android\
           bin\
           docs\
           inc\
           java\
           ...

       You would set the ALLJOYN_HOME variable to be the location:

         C:\alljoyn-3.0.2-android-sdk-dbg

 * For other projects that are part of the Peer Group Manager distribution,
   please see their README.txt files for more information.
