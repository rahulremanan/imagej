/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2012 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package imagej.plugin;

import imagej.Cancelable;
import imagej.InstantiableException;
import imagej.module.AbstractModule;
import imagej.module.ModuleException;
import imagej.util.ClassUtils;
import imagej.util.Log;

import java.util.Map;

/**
 * Module class for working with a {@link Command} instance.
 * 
 * @author Curtis Rueden
 * @author Johannes Schindelin
 * @author Grant Harris
 */
public class PluginModule<C extends Command> extends AbstractModule
	implements Cancelable
{

	/** The metadata describing the command. */
	private final PluginModuleInfo<C> info;

	/** The command instance handled by this module. */
	private final C command;

	/** Creates a plugin module for the given {@link PluginInfo}. */
	public PluginModule(final PluginModuleInfo<C> info) throws ModuleException {
		super();
		this.info = info;
		command = instantiateCommand();
		assignPresets();
	}

	/**
	 * Creates a plugin module for the given {@link PluginInfo}, around the
	 * specified {@link Command} instance.
	 */
	public PluginModule(final PluginModuleInfo<C> info, final C command) {
		super();
		this.info = info;
		this.command = command;
		assignPresets();
	}

	// -- PluginModule methods --

	/** Gets the command instance handled by this module. */
	public C getCommand() {
		return command;
	}

	// -- Module methods --

	/**
	 * Computes a preview of the command's results. For this method to do anything,
	 * the command must implement the {@link PreviewPlugin} interface.
	 */
	@Override
	public void preview() {
		if (!(command instanceof PreviewPlugin)) return; // cannot preview
		final PreviewPlugin previewPlugin = (PreviewPlugin) command;
		previewPlugin.preview();
	}

	/**
	 * Cancels the command, undoing the effects of any calls to {@link #preview()}.
	 * For this method to do anything, the command must implement the
	 * {@link PreviewPlugin} interface.
	 */
	@Override
	public void cancel() {
		if (!(command instanceof PreviewPlugin)) return; // nothing to cancel
		final PreviewPlugin previewPlugin = (PreviewPlugin) command;
		previewPlugin.cancel();
	}

	@Override
	public PluginModuleInfo<C> getInfo() {
		return info;
	}

	@Override
	public Object getDelegateObject() {
		return command;
	}

	@Override
	public Object getInput(final String name) {
		final PluginModuleItem<?> item = info.getInput(name);
		return ClassUtils.getValue(item.getField(), command);
	}

	@Override
	public Object getOutput(final String name) {
		final PluginModuleItem<?> item = info.getOutput(name);
		return ClassUtils.getValue(item.getField(), command);
	}

	@Override
	public void setInput(final String name, final Object value) {
		final PluginModuleItem<?> item = info.getInput(name);
		ClassUtils.setValue(item.getField(), command, value);
	}

	@Override
	public void setOutput(final String name, final Object value) {
		final PluginModuleItem<?> item = info.getOutput(name);
		ClassUtils.setValue(item.getField(), command, value);
	}

	// -- Object methods --

	@Override
	public String toString() {
		return command.getClass().getName();
	}

	// -- Runnable methods --

	@Override
	public void run() {
		try {
			command.run();
		}
		catch (final Throwable t) {
			Log.error(t);
		}
	}

	// -- Cancelable methods --

	@Override
	public boolean isCanceled() {
		if (!(command instanceof Cancelable)) return false;
		return ((Cancelable) command).isCanceled();
	}

	@Override
	public String getCancelReason() {
		if (!(command instanceof Cancelable)) return null;
		return ((Cancelable) command).getCancelReason();
	}

	// -- Helper methods --

	private C instantiateCommand() throws ModuleException {
		try {
			return info.createInstance();
		}
		catch (final InstantiableException exc) {
			throw new ModuleException(exc);
		}
	}

	private void assignPresets() {
		final Map<String, Object> presets = info.getPresets();
		for (final String name : presets.keySet()) {
			final Object value = presets.get(name);
			setInput(name, value);
			setResolved(name, true);
		}
	}

}
