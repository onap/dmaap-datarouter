.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

Delivery
==============

Below is a diagram of the DMaaP Data Router project docker containers and the connections between them.

.. blockdiag::


   blockdiag layers {
   orientation = portrait
   MARIADB -> DR-PROV;
   DR-PROV -> DR-NODE;
   group l1 {
	color = blue;
	label = "dr-prov Container";
	DR-PROV;
	}
   group l2 {
	color = yellow;
	label = "dr-node Container";
	DR-NODE;
	}
   group l3 {
	color = orange;
	label = "MariaDb Container";
	MARIADB;
	}

   }
