//
// GrayscaleImageTranslator.java
//

/*
ImageJ software for multidimensional image processing and analysis.

Copyright (c) 2010, ImageJDev.org.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the names of the ImageJDev.org developers nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

package imagej.legacy;

import ij.ImagePlus;
import imagej.ImageJ;
import imagej.data.Dataset;
import imagej.data.display.ImageDisplay;
import imagej.data.display.ImageDisplayService;
import imagej.ext.display.DisplayService;
import net.imglib2.img.Axes;
import net.imglib2.img.Axis;

/**
 * Translates between legacy and modern ImageJ image structures for non-RGB
 * data.
 * 
 * @author Barry DeZonia
 * @author Curtis Rueden
 */
public class GrayscaleImageTranslator implements ImageTranslator {

	@Override
	public ImageDisplay createDisplay(final ImagePlus imp) {
		return createDisplay(imp, LegacyUtils.getPreferredAxisOrder());
	}

	/**
	 * creates a {@link ImageDisplay} from an {@link ImagePlus}. If possible the
	 * ImageDisplay is made planar sharing plane references with the ImagePlus.
	 */
	@Override
	public ImageDisplay createDisplay(final ImagePlus imp,
		final Axis[] preferredOrder)
	{
		Dataset ds;
		if (preferredOrder[0] == Axes.X && preferredOrder[1] == Axes.Y) {
			ds = LegacyUtils.makeExactDataset(imp, preferredOrder);
		}
		else {
			ds = LegacyUtils.makeGrayDataset(imp, preferredOrder);
			LegacyUtils.setDatasetGrayData(ds, imp);
		}
		LegacyUtils.setDatasetMetadata(ds, imp);
		LegacyUtils.setDatasetCompositeVariables(ds, imp);

		final DisplayService displayService = ImageJ.get(DisplayService.class);
		// CTR FIXME
		final ImageDisplay display =
			(ImageDisplay) displayService.createDisplay(ds.getName(), ds);

		LegacyUtils.setDisplayLuts(display, imp);

		return display;
	}

	/**
	 * creates an {@link ImagePlus} from a {@link ImageDisplay}. The ImagePlus
	 * made shares plane references with the ImageDisplay when possible.
	 */
	@Override
	public ImagePlus createLegacyImage(final ImageDisplay display) {
		final ImageDisplayService imageDisplayService =
			ImageJ.get(ImageDisplayService.class);
		final Dataset dataset = imageDisplayService.getActiveDataset(display);
		ImagePlus imp;
		if (LegacyUtils.datasetIsIJ1Compatible(dataset)) {
			imp =	LegacyUtils.makeExactImagePlus(dataset);
		}
		else {
			imp = LegacyUtils.makeNearestTypeGrayImagePlus(dataset);
			LegacyUtils.setImagePlusGrayData(dataset, imp);
		}
		LegacyUtils.setImagePlusMetadata(dataset, imp);
		if (shouldBeComposite(dataset, imp)) {
			imp = LegacyUtils.makeCompositeImage(imp);
		}
		LegacyUtils.setImagePlusLuts(display, imp);
		return imp;
	}

	// -- helpers --

	// TODO - is this logic correct? Specifically is testing compChanCnt
	// sufficient?
	private boolean shouldBeComposite(final Dataset ds, final ImagePlus imp) {
		if (ds.getCompositeChannelCount() == 1) return false;
		final int channels = imp.getNChannels();
		if (channels < 2 || channels > 7) return false;
		return true;
	}

}
