# How to release Retable

## Maven central
To Release a new version of retable onto maven central :

1. Create and push your version tag
2. [Publishing job](https://github.com/4sh/retable/actions/workflows/publish_to_maven_central_on_create_release.yml) will be automatically start
3. Wait the end of the job
4. Go to the [Central Publisher Portal](https://central.sonatype.com/publishing/deployments) (login with your Sonatype account)
5. Validate your deployment and wait all rules passed
6. Publish your deployment

Note: legacy OSSRH (`oss.sonatype.org` / `s01.oss.sonatype.org`) was shut down by Sonatype on 2025-06-30. The workflow now deploys through the `ossrh-staging-api.central.sonatype.com` compatibility service, which requires a **Portal User Token** (see below) instead of your regular Sonatype account password, and needs an extra "finalize" API call (already wired into the workflow) for the deployment to show up in the new Portal.

### Credentials (`OSSRH_USERNAME` / `OSSRH_PASSWORD` secrets)

These must be a **Portal User Token** pair, not your Sonatype account login. Generate one from the Central Portal: log in at [central.sonatype.com](https://central.sonatype.com), go to your account, and generate a User Token — it gives you a username/password pair to use here.

## Signing key (GPG)

Releases are signed with a GPG key before being pushed to OSSRH. The key material is stored as GitHub secrets on the repo:
- `GPG_PRIVATE_KEY`: the private key, ASCII-armored (`--armor`) and base64-encoded on a single line
- `GPG_PASSPHRASE`: the key's passphrase

**The key must use the RSA algorithm.** The publish workflow runs on Gradle 6.9.3, whose bundled BouncyCastle version doesn't support Ed25519/EdDSA — the algorithm recent `gpg` versions generate by default. Using a non-RSA key fails signing with:
```
org.bouncycastle.openpgp.PGPException: unknown public key algorithm encountered
```

### Generating a new key

```bash
gpg --full-generate-key
# select "(1) RSA and RSA" explicitly (don't accept the default, which is Ed25519 on recent gpg versions)
# key size: 4096
# set an expiration date and a strong passphrase
```

### Publishing and storing the key

```bash
KEYID=<key id from `gpg --list-secret-keys --keyid-format LONG`>

# publish the public key so Sonatype/consumers can verify signatures
gpg --keyserver keyserver.ubuntu.com --send-keys $KEYID

# store the private key + passphrase as GitHub secrets
gpg --armor --export-secret-keys $KEYID | base64 -w0 | gh secret set GPG_PRIVATE_KEY --repo 4sh/retable
gh secret set GPG_PASSPHRASE --repo 4sh/retable
```

On macOS, replace `base64 -w0` with `base64 | tr -d '\n'` (BSD `base64` doesn't support `-w0`).
