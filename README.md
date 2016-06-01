# Everything we need

* Verbose/Quiet
* Error/NoError
* Server shutdown
* Continue transfer
* Empty file
* Timeout

<hr>

<b> READ </b><br><br>

Normal <br>
* Single block
* Double block
* 512 bytes (1 block)
* 132,000 bytes (255+ blocks)

Duplicate <br>
* Single block, RRQ
* Single block, WRQ
* Single block, DATA
* Single block, ACK
* Double block, RRQ
* Double block, WRQ
* Double block, DATA
* Double block, ACK
* 512 bytes (1 block)
* 132,000 bytes (255+ blocks)

Delay <br>
* Single block, RRQ
* Single block, WRQ
* Single block, DATA
* Single block, ACK
* Double block, RRQ
* Double block, WRQ
* Double block, DATA
* Double block, ACK
* 512 bytes (1 block)
* 132,000 bytes (255+ blocks)

Lost <br>
* Single block, RRQ
* Single block, WRQ
* Single block, DATA
* Single block, ACK
* Double block, RRQ
* Double block, WRQ
* Double block, DATA
* Double block, ACK
* 512 bytes (1 block)
* 132,000 bytes (255+ blocks)

<hr>

<b> WRITE </b><br><br>

Normal <br>
* Single block, RRQ
* Single block, WRQ
* Single block, DATA
* Single block, ACK
* Double block, RRQ
* Double block, WRQ
* Double block, DATA
* Double block, ACK
* 512 bytes (1 block)
* 132,000 bytes (255+ blocks)

Duplicate <br>
* Single block, RRQ
* Single block, WRQ
* Single block, DATA
* Single block, ACK
* Double block, RRQ
* Double block, WRQ
* Double block, DATA
* Double block, ACK
* 512 bytes (1 block)
* 132,000 bytes (255+ blocks)

Delay <br>
* Single block, RRQ
* Single block, WRQ
* Single block, DATA
* Single block, ACK
* Double block, RRQ
* Double block, WRQ
* Double block, DATA
* Double block, ACK
* 512 bytes (1 block)
* 132,000 bytes (255+ blocks)

Lost <br>
* Single block, RRQ
* Single block, WRQ
* Single block, DATA
* Single block, ACK
* Double block, RRQ
* Double block, WRQ
* Double block, DATA
* Double block, ACK
* 512 bytes (1 block)
* 132,000 bytes (255+ blocks)

# Problems from TA test

1. Fix file creation
2. stops after one
3. implement 512, 0bytes case
4. breaks at 8k
5. shutdown server
6. timeout client if no server
7. try and few times and then timeout
8. client shutdowns with user input q
