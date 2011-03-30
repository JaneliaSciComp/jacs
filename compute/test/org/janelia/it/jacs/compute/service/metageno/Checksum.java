/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.compute.service.metageno;

/**
 * Calcualtes CRC64 and MD5 checksums
 * User: jgoll
 * Date: Mar 31, 2009
 * Time: 12:15:22 PM
 *
 */


import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksum {

	private long[] crc64Array;
	public static Checksum instance;

	private Checksum() {
		crc64Array = new long[256];
		for (int i = 0; i <= 255; ++i) {
			long k = i;
			for (int j = 0; j < 8; ++j) {
				if ((k & 1) != 0) {
					k = (k >>> 1) ^ 0xd800000000000000l;
				} else {
					k = k >>> 1;
				}
			}
			crc64Array[i] = k;
		}
	}

	public static Checksum getInstance() {
		if(instance==null)
			instance = new Checksum();
		return instance;
	}

	public String calcMD5(String sequence) {
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		md5.update(sequence.toString().toUpperCase().getBytes());
		BigInteger md5hash = new BigInteger(1, md5.digest());
		String sequence_md5 = md5hash.toString(16);
		while (sequence_md5.length() < 32) {
			sequence_md5 = "0" + sequence_md5;
		}
		return sequence_md5;
	}

	public String calcCRC64(String sequence) {
		sequence = sequence.toUpperCase();

		long crc64Number = 0;
		for (int i = 0; i < sequence.length(); ++i) {
			char symbol = sequence.charAt(i);
			long a = (crc64Number >>> 8);
			long b = (crc64Number ^ symbol) & 0xff;
			crc64Number = a ^ crc64Array[(int) b];
		}

		String crc64String = Long.toHexString(crc64Number).toUpperCase();
		StringBuffer crc64 = new StringBuffer("0000000000000000");
		crc64.replace(crc64.length() - crc64String.length(), crc64.length(),
				crc64String);

		return crc64.toString();
	}

}


