---
title: AsokanSchunter
permalink: wiki/AsokanSchunter/
layout: wiki
---

The link does not grant access to the whole document, however I
recovered it by other means. This is a recent review paper by
specialists, hence a good entry to the topic. The comments here are
biased towards SXP.

Fair Exchange
=============

Describes fair exchange protocols as ways to exchange things between
distrustful parties in such a way that either both parties receive what
they expect or neither parties does. Includes receipts for payments,
certified mail, and contract signatures more specfically.

Types of Fair Exchanges (FX)
============================

With a Trusted Third Party (TTP)
--------------------------------

Requires a referee. In the setting of SXP, maybe the market could maybe
play such a role. The protocols that do that have a common pattern. As
usual Alice (A) and Bob (B) are the normal parties, and Trent (T) is the
Trusted Third Party. They seek to exchange M\_A and M\_B. Moreover see
[here](http://en.wikipedia.org/wiki/Security_protocol_notation) for the
notation.

\[A\rightarrow B:\{M_A\}_{K}\]

\[A\rightarrow T:K\]

\[B\rightarrow T:M_B\]

\[T\rightarrow B:K\]

\[T\rightarrow B:K\]

Being Optimistic
----------------

Does not require a referee, unless there is a conflict.

### Optimistic FX

### Verifiable escrow of digital signatures

### Verifiable escrow using pre-issued coupons

Exchanging in parts
-------------------