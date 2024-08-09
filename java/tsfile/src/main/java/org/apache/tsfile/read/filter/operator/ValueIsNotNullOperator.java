/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tsfile.read.filter.operator;

import org.apache.tsfile.block.column.Column;
import org.apache.tsfile.exception.NotImplementedException;
import org.apache.tsfile.file.metadata.IMetadata;
import org.apache.tsfile.file.metadata.statistics.Statistics;
import org.apache.tsfile.read.common.block.TsBlock;
import org.apache.tsfile.read.filter.basic.CompareNullFilter;
import org.apache.tsfile.read.filter.basic.Filter;
import org.apache.tsfile.read.filter.basic.OperatorType;
import org.apache.tsfile.utils.Binary;
import org.apache.tsfile.utils.ReadWriteIOUtils;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Optional;

import static org.apache.tsfile.utils.ReadWriteIOUtils.ClassSerializeId.NULL;

public class ValueIsNotNullOperator extends CompareNullFilter {

  public ValueIsNotNullOperator(int measurementIndex) {
    super(measurementIndex);
  }

  public ValueIsNotNullOperator(ByteBuffer buffer) {
    super(buffer);
  }

  @Override
  public boolean satisfy(long time, Object value) {
    return value != null;
  }

  @Override
  public boolean satisfyBoolean(long time, boolean value) {
    return true;
  }

  @Override
  public boolean satisfyInteger(long time, int value) {
    return true;
  }

  @Override
  public boolean satisfyLong(long time, long value) {
    return true;
  }

  @Override
  public boolean satisfyFloat(long time, float value) {
    return true;
  }

  @Override
  public boolean satisfyDouble(long time, double value) {
    return true;
  }

  @Override
  public boolean satisfyBinary(long time, Binary value) {
    return value != null;
  }

  @Override
  public boolean[] satisfyTsBlock(boolean[] selection, TsBlock tsBlock) {
    Column valueColumn = tsBlock.getValueColumns()[measurementIndex];
    boolean[] satisfyInfo = new boolean[selection.length];
    System.arraycopy(selection, 0, satisfyInfo, 0, selection.length);
    for (int i = 0; i < selection.length; i++) {
      if (selection[i]) {
        satisfyInfo[i] = !valueColumn.isNull(i);
      }
    }
    return satisfyInfo;
  }

  @Override
  public Filter reverse() {
    return new ValueIsNullOperator(measurementIndex);
  }

  @Override
  public OperatorType getOperatorType() {
    return OperatorType.VALUE_IS_NOT_NULL;
  }

  @Override
  public boolean valueSatisfy(Object value) {
    return false;
  }

  @Override
  public boolean canSkip(Statistics<? extends Serializable> statistics) {
    throw new NotImplementedException();
  }

  @Override
  public boolean canSkip(IMetadata metadata) {
    Optional<Statistics<? extends Serializable>> statistics =
        metadata.getMeasurementStatistics(measurementIndex);

    if (!statistics.isPresent()) {
      // the measurement isn't in this block so all values are null.
      // null is always equal to null
      return true;
    }

    // we are looking for records where v notEq(null)
    // so, if this is a column of all nulls, we can drop it
    return statistics.get().getCount() == 0;
  }

  @Override
  public boolean allSatisfy(Statistics<? extends Serializable> statistics) {

    throw new NotImplementedException();
  }

  @Override
  public boolean allSatisfy(IMetadata metadata) {
    Optional<Statistics<? extends Serializable>> statistics =
        metadata.getMeasurementStatistics(measurementIndex);

    if (!statistics.isPresent()) {
      // block cannot match
      return false;
    }

    return !metadata.hasNullValue(measurementIndex);
  }

  @Override
  public ReadWriteIOUtils.ClassSerializeId getClassSerializeId() {
    return NULL;
  }
}
