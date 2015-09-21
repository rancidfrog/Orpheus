/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.index.scanner;

import org.opensilk.music.index.IndexComponent;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 9/16/15.
 */
@ScannerScope
@Component(
        dependencies = IndexComponent.class,
        modules = ScannerModule.class
)
public interface ScannerComponent {
    Func1<IndexComponent, ScannerComponent> FACTORY = new Func1<IndexComponent, ScannerComponent>() {
        @Override
        public ScannerComponent call(IndexComponent indexComponent) {
            return DaggerScannerComponent.builder()
                    .indexComponent(indexComponent)
                    .build();
        }
    };
    void inject(ScannerService service);
}
