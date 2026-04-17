Never change or pass a Hytale server auth mode override from this repository.

Rules:
- Use the server jar or `start.bat` defaults as-is.
- Do not patch launch arguments to force offline, online, TCP-only, UDP-only, or any alternate auth behavior from this repo.
- Test scripts, bot harness scripts, deployment scripts, and setup notes in this repo must preserve the server's default auth mode.

Reason:
- Changing auth mode from this repo breaks local client validation and invalidates gameplay testing.
