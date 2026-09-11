# Court SMS Forwarder

Android app that detects court-related SMS (e.g. 12368 delivery notices) and forwards them to your email.

Package: `cn.senrylee.courtsms`  
Repo: https://github.com/SenryLee/CourtSmsForwarder

## What it does

Court SMS → local match against built-in rules → email.

UI keeps only **Forward Logs**, **Forward Rules**, and **Settings** (email + auth code).

## Quick start

1. Install from [Releases](https://github.com/SenryLee/CourtSmsForwarder/releases)
2. Grant SMS / notification permissions; disable battery optimization if needed
3. In Settings, enter email and SMTP auth code (QQ / Foxmail recommended)
4. Saving auto-seeds court rules and starts forwarding

## Note

This is a standalone product focused on court SMS. It is not a general-purpose multi-channel SMS forwarder.
