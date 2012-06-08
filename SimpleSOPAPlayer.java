/*
 *	SimpleSOPAPlayer.java
 
 Copyright (c) 2012, AIST
 
 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in
 the Software without restriction, including without limitation the rights to
 use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 of the Software, and to permit persons to whom the Software is furnished to do
 so, subject to the following conditions:
 
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
 
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 
 *
 */

import java.io.*;
import java.io.IOException;
import java.util.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SimpleSOPAPlayer
{
	private static final int	EXTERNAL_BUFFER_SIZE = 65536;

	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			printUsageAndExit();
		}

		int nNum = 0;
		int nSampleRate = 44100;
		int nChannels = 2;
		int nBit = 16;
		int nOverlap = 4;										// frame overlapp
		int nCnt,nLen = 4;
		int[] nByte = new int[nLen];
		int[] nFmt = new int[nLen - 1];
		int nTerm0[] = {82,73,70,70};				// RIFF
		int nTerm1[] = {83,79,80,65};				// SOPA
		int nTerm2[] = {102,109,116};				// fmt
//		int nTerm3[] = {100,97,116,97};				// data
		short[] sHrtf = new short[36864];
		short[] sPhase = new short[36864];
		String	strFilename = args[0];
		File	soundFile = new File(strFilename);
		FileInputStream inStream = null;
		AudioFormat audioFormat = null;
	
		DataInputStream din = null;
		try
		{
			din = new DataInputStream(new FileInputStream("hrtf512.bin"));
			for(nNum = 0;nNum < 36864;nNum ++) 
			{															// BigEndian to LittleEndian
				sHrtf[nNum] = (short)((din.readByte() & 0xff) + (din.readByte() << 8));
			}
			din.close();
		}
		catch(Exception e)
		{
			System.out.println("HRTF data error! : " + e);
			System.exit(1);
		}

		try
		{
			din = new DataInputStream(new FileInputStream("phase512.bin"));
			for(nNum = 0;nNum < 36864;nNum ++) 
			{															// BigEndian to LittleEndian
				sPhase[nNum] = (short)((din.readByte() & 0xff) + (din.readByte() << 8));
			}
			din.close();
		}
		catch(Exception e)
		{
			System.out.println("PHASE data error! : " + e);
			System.exit(1);
		}
		System.out.println("HRTF data number " + nNum + "\n");

		try
		{
			inStream = new FileInputStream(soundFile);
			for(nCnt = 0;nCnt < nLen;nCnt ++)
			{
				nByte[nCnt] = inStream.read();
				if(nByte[nCnt] == -1)
				{
					inStream.close();
					System.exit(1);
				}
			}
			if(!Arrays.equals(nByte,nTerm0))
			{
				System.out.println("File format error!");
				inStream.close();
				System.exit(1);
			}
			System.out.println("RIFF OK");
			for(nCnt = 0;nCnt < nLen;nCnt ++)
			{
				nByte[nCnt] = inStream.read();
				if(nByte[nCnt] == -1)
				{
					inStream.close();
					System.exit(1);
				}
			}
			for(nCnt = 0;nCnt < nLen;nCnt ++)
			{
				nByte[nCnt] = inStream.read();
				if(nByte[nCnt] == -1)
				{
					inStream.close();
					System.exit(1);
				}
			}
			if(!Arrays.equals(nByte,nTerm1))
			{
				System.out.println("File format error!");
				inStream.close();
				System.exit(1);
			}
			System.out.println("SOPA OK");
			for(nCnt = 0;nCnt < nLen - 1;nCnt ++)
			{
				nFmt[nCnt] = inStream.read();
				if(nFmt[nCnt] == -1)
				{
					inStream.close();
					System.exit(1);
				}
			}
			if(!Arrays.equals(nFmt,nTerm2))
			{
				System.out.println("File format error!");
				inStream.close();
				System.exit(1);
			}
			System.out.println("fmt OK");
			inStream.read();				
			
			nBit = inStream.read();
			if(nBit != 16)
			{
				System.out.println("Data are not 16-bit!");
				inStream.close();
				System.exit(1);
			}
			for(nCnt = 0;nCnt < 3;nCnt ++)
				inStream.read();
			if(inStream.read() != 1)
			{
				System.out.println("Data are not PCM!");
				inStream.close();
				System.exit(1);
			}
			inStream.read();
			nOverlap = inStream.read();
			if(nOverlap != 2 && nOverlap != 4)
			{
				System.out.println("Error! overlap number should be 2 or 4");
				inStream.close();
				System.exit(1);
			}
			inStream.read();
			nSampleRate = inStream.read();
			nSampleRate += inStream.read() * 256;
			for(nCnt = 0;nCnt < 10;nCnt ++)
			{
				inStream.read();
			}
			for(nCnt = 0;nCnt < nLen;nCnt ++)
			{
				nByte[nCnt] = inStream.read();
				if(nByte[nCnt] == -1)
				{
					inStream.close();
					System.exit(1);
				}
			}
			System.out.println("SOPA file version = " + nByte[3] + "." + nByte[2] + "." + nByte[1] + "." + nByte[0]);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,nSampleRate,nBit,nChannels,
			nChannels * nBit / 8,nSampleRate,false);

		SourceDataLine	line = null;
		DataLine.Info	info = new DataLine.Info(SourceDataLine.class,
												 audioFormat);
		try
		{
			line = (SourceDataLine) AudioSystem.getLine(info);

			line.open(audioFormat);
		}
		catch (LineUnavailableException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		line.start();

		final int iFIN = 16384;
		final int iBYTE = nBit / 8;
		int nRatio = 44100 / nSampleRate;
		int nSize = 2048;									// FFT window size
		int nFrameNum;										// number of frames
		int nProc = nSize / nOverlap;							// frame increment
		int nRem = nSize - nProc;
		int	nBytesRead = 0;
		int nBytesWritten = 0;
		byte[]	abData = new byte[EXTERNAL_BUFFER_SIZE];	// data array
		byte[] bRet = new byte[2];
		int[]	nAngle = new int[nSize];
		short[][] sData = new short[2][EXTERNAL_BUFFER_SIZE / 2];
		short[][] sVal = new short[2][EXTERNAL_BUFFER_SIZE / 4];
		short sDum[][] = new short[2][iFIN];

		double dRealL[] = new double[nSize];
		double dImageL[] = new double[nSize];
		double dRealR[] = new double[nSize];
		double dImageR[] = new double[nSize];
		double dPow[] = new double[nSize];
		double dPh[] = new double[nSize];

		int nSect,nInt,nSamplesWritten,nOffset,nNumImage,nTmp,nHlfSize;
		short sSample,sTmp;
		double dSpL,dSpR,dSpImageL,dSpImageR;
		double dWindow;										// window function
		double dTmp,dPhaseL,dPhaseImageL,dPhaseR,dPhaseImageR;

		fft test = new fft();

		nSamplesWritten = 0;
		try
		{
			for(nCnt = 0;nCnt < 4;nCnt ++)
				inStream.read();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		while (nBytesRead != -1)
		{
			try
			{
				nBytesRead = inStream.read(abData, 0, abData.length);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				break;
			}
			if(nSamplesWritten == 0)
			{
				nInt = 5;
				sSample = 1;
				while(sSample > 0)
				{
					sSample = (short)abData[nInt];
					nInt += 4;
				}
				nSize = nInt - 5;
				System.out.println("Frame size is " + nSize + "\n");
				nProc = nSize / nOverlap;
				nRem = nSize - nProc;
				nRatio *= nSize / 512;
			}
			nHlfSize = nSize / 2;							// half of FFT window size
			nSect = nBytesRead / iBYTE / nChannels;			// samples per section
			if(nBytesWritten > 0)
				nSect += nRem;
			nFrameNum = nSect / nProc;						// number of frames
			if(nBytesRead == EXTERNAL_BUFFER_SIZE)
				nFrameNum -= nOverlap - 1;
			test.iTap = nSize;							// FFT window size

//			dPow[nSize] = dPh[nSize] = 0;
			if(nBytesRead >= 0)
			{
				rearrangeSOPA(abData,sVal,false);
				for(nInt = 0;nInt < nFrameNum;nInt ++)
				{
					nCnt = nProc * nInt;
					if(nBytesWritten == 0)
					{
						for(nNum = 0;nNum < nSize;nNum ++)
						{
							dRealR[nNum] = (double)sVal[1][nCnt + nNum];		// signal in the right channel
						}
					}
					else
					{
						for(nNum = 0;nNum < nSize;nNum ++)
						{
							if(nCnt + nNum < nRem)
								dRealR[nNum] = (double)sDum[1][nCnt + nNum];		
/*							else if(nCnt + nNum + nRem >= EXTERNAL_BUFFER_SIZE / 4)
								dRe[0][nNum] = 0;	*/
							else
								dRealR[nNum] = (double)sVal[1][nCnt + nNum - nRem];		// signal in the right channel
						}
					}
					if(test.fastFt(dRealR,dImageR,dPow,dPh,false))									// FFT
					{
						nAngle[nHlfSize] = 0;
						for(nNum = 0;nNum < nHlfSize;nNum ++)
						{
							int iBer = nNum / 2;
							int iFreq = nNum / nRatio;
							if(nBytesWritten == 0)
							{
								sTmp = sVal[0][nCnt + iBer];
							}
							else if(nInt < nOverlap - 1)
							{
								sTmp = sDum[0][nCnt + iBer];
							}	
							else
							{
								sTmp = sVal[0][nCnt - nRem + iBer];
							}	
							if(nNum % 2 == 0)
							{
								if(sTmp < 0)
									nAngle[nNum] = -sTmp / 256;
								else
									nAngle[nNum] = sTmp / 256;
							}
							else
							{
								if(sTmp < 0)
									nAngle[nNum] = (-sTmp) % 256;
								else
									nAngle[nNum] = sTmp % 256;
							}
							if(nAngle[nNum] <= 0)								// in the case of f_0 (Hz)
							{
								dSpR = dPow[nNum];
								dSpL = dPow[nNum];
								dSpImageL = dPow[nNum];
								dSpImageR = dPow[nNum];
								dPhaseL = dPh[nNum];
								dPhaseR = dPh[nNum];
								dPhaseImageL = dPh[nNum];
								dPhaseImageR = dPh[nNum];
							}
							else
							{
								nAngle[nNum] -= 1;
								//							nAngle[nNum] = 18;
								if(nAngle[nNum] > 71)
								{
									nAngle[nNum] -= 72;
								}
								else if(nAngle[nNum] < 0)
								{
									nAngle[nNum] += 72;
								}
								nOffset = 512 * (71 - nAngle[nNum]) + iFreq;
								nNumImage = 512 * (71 - nAngle[nNum]) + nSize - iFreq;
								if(nNumImage >= 36864)
									nNumImage -= 36864;
								else if(nNumImage < 0)
									nNumImage += 36864;
								if(nOffset >= 36864)
									nOffset -= 36864;
								else if(nOffset < 0)
									nOffset += 36864;

								dTmp = (double)sHrtf[nOffset];
								dSpL = dPow[nNum] * dTmp / 2048.0;
								dTmp = (double)sPhase[nOffset];
								dPhaseL = dPh[nNum] + dTmp / 10000.0;
								dTmp = (double)sHrtf[nNumImage];
								dSpImageL = dPow[nSize - nNum] * dTmp / 2048.0;
								dTmp = (double)sPhase[nNumImage];
								dPhaseImageL = dPh[nSize - nNum] + dTmp / 10000.0;

								nOffset = 512 * nAngle[nNum] + iFreq;
								nNumImage = 512 * nAngle[nNum] + nSize - iFreq;
								if(nNumImage >= 36864)
									nNumImage -= 36864;
								else if(nNumImage < 0)
									nNumImage += 36864;
								if(nOffset >= 36864)
									nOffset -= 36864;
								else if(nOffset < 0)
									nOffset += 36864;

								dTmp = (double)sHrtf[nOffset];
								dSpR = dPow[nNum] * dTmp / 2048.0;
								dTmp = (double)sPhase[nOffset];
								dPhaseR = dPh[nNum] + dTmp / 10000.0;
								dTmp = (double)sHrtf[nNumImage];
								dSpImageR = dPow[nSize - nNum] * dTmp / 2048.0;
								dTmp = (double)sPhase[nNumImage];
								dPhaseImageR = dPh[nSize - nNum] + dTmp / 10000.0;
							}

							dRealL[nNum] = dSpL * Math.cos(dPhaseL);
							dRealR[nNum] = dSpR * Math.cos(dPhaseR);
							dImageL[nNum] = dSpL * Math.sin(dPhaseL);
							dImageR[nNum] = dSpR * Math.sin(dPhaseR);
							if(nNum != 0)
							{
								dRealL[nSize - nNum] = dSpImageL * Math.cos(dPhaseImageL);
								dRealR[nSize - nNum] = dSpImageR * Math.cos(dPhaseImageR);
								dImageL[nSize - nNum] = dSpImageL * Math.sin(dPhaseImageL);
								dImageR[nSize - nNum] = dSpImageR * Math.sin(dPhaseImageR);
							}
							/*							dRe[1][nNum] = dRe[0][nNum];
							 dIm[1][nNum] = dIm[0][nNum];
							 dRe[1][nSize - nNum] = dRe[0][nSize - nNum];
							 dIm[1][nSize - nNum] = dIm[0][nSize - nNum];	*/
						}
						dRealL[nHlfSize] = dRealR[nHlfSize];
						dImageL[nHlfSize] = dImageR[nHlfSize];
						if(test.fastFt(dRealL,dImageL,dPow,dPh,true))										// inverse FFT (left channel)
						{
							if(test.fastFt(dRealR,dImageR,dPow,dPh,true))									// inverse FFT (right channel)
							{
								for(nNum = 0;nNum < nSize;nNum ++)
								{
									dWindow = (1 - Math.cos(2 * Math.PI * (double)nNum / (double)nSize)) / 4;
									dRealL[nNum] *= dWindow;
									dRealR[nNum] *= dWindow;	
									sData[0][nCnt + nNum] += dRealL[nNum];
									sData[1][nCnt + nNum] += dRealR[nNum];
								}
							}
							else
								System.out.println("inverse FFT (right) Error\n");
						}
						else
							System.out.println("inverse FFT (left) Error\n");
					}
					else
					{
						System.out.println(" forward FFT Error\n");
						System.exit(0);
					}
				}

				bRet[0] = bRet[1] = 0;
				for(nInt = 0;nInt < nSect - nRem;nInt ++)
				{
					intToByte((int)sData[0][nInt],bRet);
					abData[nInt * 4] = bRet[0];
					abData[nInt * 4 + 1] = bRet[1];
					intToByte((int)sData[1][nInt],bRet);
					abData[nInt * 4 + 2] = bRet[0];
					abData[nInt * 4 + 3] = bRet[1];	
				}
				if(nBytesRead < EXTERNAL_BUFFER_SIZE)
					nBytesWritten += line.write(abData, 0, nBytesRead + nRem * 4);
				else if(nBytesWritten != 0)
					nBytesWritten += line.write(abData, 0, nBytesRead);
				else
					nBytesWritten += line.write(abData, 0, nBytesRead - nRem * 4);
				nTmp = nSect - nRem;
				for(nInt = 0;nInt < nSect;nInt ++)
				{
					if(nInt < nRem)
					{
						sData[0][nInt] = sData[0][nInt + nTmp];
						sData[1][nInt] = sData[1][nInt + nTmp];
						if(nSamplesWritten == 0)
						{
							sDum[0][nInt] = sVal[0][nInt + nTmp];
							sDum[1][nInt] = sVal[1][nInt + nTmp];
						}
						else
						{
							sDum[0][nInt] = sVal[0][nInt + nTmp - nRem];
							sDum[1][nInt] = sVal[1][nInt + nTmp - nRem];
						}
					}
					else
					{
						sData[0][nInt] = sData[1][nInt] = 0;
					}
				}
				nSamplesWritten = nBytesWritten / iBYTE / nChannels;
			}
		}
		System.out.println(nSamplesWritten + "samples were played.\n");

		line.drain();

		line.close();
		try
		{
			inStream.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

	private static void rearrangeSOPA(byte bDt[], short sDt[][],boolean littleOrBig)
	{
		int iNum;
		int nTmp,iLow;
		int iV;

		for(iNum = 0;iNum < bDt.length;iNum += 2)
		{
			if(!littleOrBig)
			{
				nTmp = bDt[iNum + 1] << 8;
				iLow = bDt[iNum];
				iV = nTmp + (iLow & 0x000000FF);
			}
			else
			{
				nTmp = bDt[iNum] << 8;
				iLow = bDt[iNum + 1];
				iV = nTmp + (iLow & 0x000000FF);
			}
			if((iNum / 2) % 2 == 0)
			{
				sDt[0][iNum / 4] = (short)iV;						// information of direction
			}
			else
				sDt[1][(iNum - 2) / 4] = (short)iV;					// PCM data
		}
	}

	private static void intToByte(int iDt,byte bRet[])
	{
		int iV = iDt;

		bRet[0] = (byte)(iDt & 0x00FF);
		bRet[1] = (byte)(iDt >>> 8 & 0x00ff);
	}

	private static void printUsageAndExit()
	{
		out("SimpleSOPAPlayer: usage:");
		out("\tjava SimpleSOPAPlayer <SOPA file>");
		System.exit(1);
	}


	private static void out(String strMessage)
	{
		System.out.println(strMessage);
	}
}



/*** SimpleSOPAPlayer.java ***/

