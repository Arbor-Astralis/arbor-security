# arbor-security

A Discord bot to provide security utilities for [Arbor Astralis](https://disboard.org/server/1210865909321957376).

## Functions

* [ ] Screen new members
  * [ ] Auto-kick new members whose account age is less than N days old, default to 30.
  * [ ] Auto-kick new members with no Discord profile image.
  * [ ] Ability to grant exemptions through a slash command.
  * [ ] Announce new member entry in `#general` only when they pass screening.
  * [ ] Automatically ban new members within 24 hours who join VC channels with voice/camera activity enabled.
  * [ ] Provide an audit trail.

  
* [ ] Automatically remove new members from the server if they have not been active after 1 month of joining.
  * **Remove automatically with no message to user:**
    * Members who joined without sending a single message.
    * Members who joined, whose messaging activity tapered off within the first 7 days.
    * Provide an audit trail.
* [ ] Track users whose last message is older than 1 month & notify admin/owner.
  * [ ] Ability to grant exemptions through a slash command.


* [ ] Automatically ban members who are spamming the server
  * [ ] Auto-ban non-staff members sending mentions at a rate of 5+ per 10 seconds, across any channel.   
  * [ ] Provide an audit trail.


* [ ] Keep `#introductions` up-to-date: remove posting 3 days after member leaves server, or when they are banned.
  * [ ] Provide an audit trail.