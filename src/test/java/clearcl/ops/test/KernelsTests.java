package clearcl.ops.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import clearcl.ClearCL;
import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.ClearCLBackends;
import clearcl.enums.HostAccessType;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.KernelAccessType;
import clearcl.enums.MemAllocMode;
import clearcl.ops.kernels.CLKernelException;
import clearcl.ops.kernels.CLKernelExecutor;
import clearcl.ops.kernels.Kernels;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author nico
 */
public class KernelsTests
{
  private ClearCLContext gCLContext;
  private CLKernelExecutor gCLKE;
  final long xSize = 1024;
  final long ySize = 1024;
  long[] dimensions2D =
  { xSize, ySize };

  @Before
  public void initKernelTests() throws IOException
  {
    ClearCLBackendInterface lClearCLBackend =
                                            ClearCLBackends.getBestBackend();

    ClearCL lClearCL = new ClearCL(lClearCLBackend);

    ClearCLDevice lBestGPUDevice = lClearCL.getBestGPUDevice();

    gCLContext = lBestGPUDevice.createContext();

    gCLKE = new CLKernelExecutor(gCLContext,
                                 clearcl.ocllib.OCLlib.class);
  }

  @After
  public void cleanupKernelTests() throws IOException
  {

    gCLKE.close();

    gCLContext.close();
  }

  @Test
  public void testBlurImage() throws IOException
  {

    ClearCLBuffer lCLsrcBuffer =
                               gCLContext.createBuffer(MemAllocMode.Best,
                                                       HostAccessType.ReadWrite,
                                                       KernelAccessType.ReadWrite,
                                                       1,
                                                       NativeTypeEnum.UnsignedShort,
                                                       dimensions2D);

    ClearCLBuffer lCldstBuffer = gCLKE.createCLBuffer(lCLsrcBuffer);

    try
    {
      Kernels.blur(gCLKE, lCLsrcBuffer, lCldstBuffer, 4.0f, 4.0f);
    }
    catch (CLKernelException clkExc)
    {
      Assert.fail(clkExc.getMessage());
    }
  }
  
  @Test
  public void testMinMaxBuffer()
  {
    ClearCLBuffer lCLBuffer = gCLKE.createCLBuffer(new long[]
    { 2048 * 2048 + 1 }, NativeTypeEnum.Float);

    OffHeapMemory lBuffer =
                          OffHeapMemory.allocateFloats(lCLBuffer.getLength());

    float lJavaMin = Float.POSITIVE_INFINITY;
    float lJavaMax = Float.NEGATIVE_INFINITY;
    for (int i = 0; i < lCLBuffer.getLength(); i++)
    {
      float lValue = 1f / (1f + i);
      lJavaMin = Math.min(lJavaMin, lValue);
      lJavaMax = Math.max(lJavaMax, lValue);
      lBuffer.setFloatAligned(i, lValue);
    }

    // System.out.println("lJavaMin=" + lJavaMin);
    // System.out.println("lJavaMax=" + lJavaMax);

    lCLBuffer.readFrom(lBuffer, true);
    try
    {
      float[] lOpenCLMinMax = Kernels.minMax(gCLKE, lCLBuffer, 128);
      assertEquals(lJavaMin, lOpenCLMinMax[0], 0.0001);
      assertEquals(lJavaMax, lOpenCLMinMax[1], 0.0001);

    }
    catch (CLKernelException clkExc)
    {
      Assert.fail(clkExc.getMessage());
    }

    lCLBuffer.close();
  }


  @Test
  public void testMinMaxImage()
  {
    ClearCLImage lCLImage = gCLKE.createCLImage(dimensions2D, ImageChannelDataType.Float);
    
    long size = lCLImage.getWidth() * lCLImage.getHeight();
    OffHeapMemory lBuffer =
                          OffHeapMemory.allocateFloats(size);

    float lJavaMin = Float.POSITIVE_INFINITY;
    float lJavaMax = Float.NEGATIVE_INFINITY;
    for (int i = 0; i < size; i++)
    {
      float lValue = 1f / (1f + i);
      lJavaMin = Math.min(lJavaMin, lValue);
      lJavaMax = Math.max(lJavaMax, lValue);
      lBuffer.setFloatAligned(i, lValue);
    }

    // System.out.println("lJavaMin=" + lJavaMin);
    // System.out.println("lJavaMax=" + lJavaMax);

    lCLImage.readFrom(lBuffer, true);
    try
    {
      float[] lOpenCLMinMax = Kernels.minMax(gCLKE, lCLImage, 128);
      assertEquals(lJavaMin, lOpenCLMinMax[0], 0.0001);
      assertEquals(lJavaMax, lOpenCLMinMax[1], 0.0001);

    }
    catch (CLKernelException clkExc)
    {
      Assert.fail(clkExc.getMessage());
    }

    lCLImage.close();
  }
  
  
}