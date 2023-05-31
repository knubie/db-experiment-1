A traditional web app architecture looks like this:

DB <-> Server <-> Client

The client requests data from the server. The server performs authorization checks, then requests that data from the DB. The DB then fetchs thedata and gives it back to the server which then sends it back to the client.

This picture looks similar for writes.

Why does the client need the server to authorize content that the client has created? Suppose the client writes a new blog post. They send that blog post to the server, the server verifies that the user can save that blog post, then sends it over to the database. Some time later the user logs back on using the same device and requests the content from the server that they just created. Why does the client need to request that data from the server?

Outsource identity to email, phone number, other verifiable service.

Philosphy that users own their data, as such authorization needs to be at the database layer. Cannot enforce authorization from a centralized server while
Each "document" goes in its *own* database that has its own permissioning system

User databases are stored locally and replicated to remote.

Only admins can create databases?

Server DB requires a user for external writes (including syncing).
requires user for external connection. internal connection doesn't require a user.
User is an ID and secret.

Client DB does not requires a user, but is automatically authed.

Client DB is authless by default.

Handle authorization on a centralized server doesn't make sense with a distributed database.

Documents can reference data from other databases with 'idents'

Post:
{:post/content "Some string
 :post/author [:global/users :098]
 :post/replies [[:user-id1/journal :123]
                [:user-id2/journal :456]]}

Database schema for a typical twitter style application

Users can create new, update itself, can read parts of other users, and delete itself, 
:global/users

:user-id/feed
public - read
user - read/write

:user-id/likes
public - read
user - read/write

:user-id/inbox
public - write (must be a user of the system? what system?)
user - read

:user-id/settings (includes password)
user - read/write

:user-id/profile
public - read
user - read/write


:matthew.steedman/profile
[:profile/name "Matthew Steedman" :matthew.steedman]


:matthew.steedman/settings
[]


:matthew.steedman/inbox
[1 :inbox/subject "Not spam" :sam.blackman]
[1 :inbox/content "hi there" :sam.blackman]

[2 :inbox/subject "Hello?"            :sam.blackman]
[2 :inbox/content "just following up" :sam.blackman]


:matthew.steedman/inbox-reads
[1 :inbox-reads/belongs-to [:matthew.steedman/inbox 1]]
[1 :inbox-reads/read?      true]

[2 :inbox-reads/belongs-to [:matthew.steedman/inbox 2]]
[2 :inbox-reads/read?      false]


:matthew.steedman/feed
[1 :feed/content "eating an apple"]

[2 :feed/content "no u!"]
[2 :feed/replying-to [:sam.blackman/feed 1]



Mochi

:global/users
public - write

:user-id/cards
user - read/write

:user-id/notes
user - read/write

user-id/published-notes
public - read
user - read/write


# Replication

1. target database checks it's replication logs for the source database
1. replication log consists of a database id and latest transaction

