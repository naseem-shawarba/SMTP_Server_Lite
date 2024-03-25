# SMTP Server Lite

## Overview
SMTP Server Lite is a lightweight implementation of an SMTP (Simple Mail Transfer Protocol) server in Java using NIO (Non-blocking I/O). It supports a reduced set of SMTP commands including HELO, MAIL FROM, RCPT TO, DATA, HELP, and QUIT. The Server stores the Received emails efficiently in files following a specific naming convention. The naming convention for the files is as follows: 

< receiver >/< sender >_< message_id >




## Features
- Implementation of a reduced version of the SMTP protocol.
- Utilizes Java NIO for non-blocking I/O operations.
- Supports concurrent handling of multiple emails.
- Efficient storage of received emails in files.
- Compatible with a simple SMTP client for testing purposes.

## Installation
- Clone the repository: `https://github.com/naseem-shawarba/SMTP_Server_Lite.git`
- Compile the project using your preferred Java IDE or command-line tools.

## Usage
1. Run the SMTP Server Lite.
2. Use a compatible SMTP client to send test emails.
3. Check the specified directory for stored emails.
