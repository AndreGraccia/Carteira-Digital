import { useLayoutEffect, useMemo, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { Box, Stack, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';
import { visuallyHidden } from '../wallet-ui';
import { compactMoney, dayLabel, money } from '../../utils/wallet';

const HEIGHT = 240;
const PAD = { top: 16, right: 16, bottom: 28, left: 72 };

/**
 * The container width, measured before the first paint: a guessed 640 px would overflow a 375 px
 * screen for one frame and then jump. No floor either, so a 320 px screen gets a 256 px chart.
 */
function useWidth() {
  const ref = useRef(null);
  const [width, setWidth] = useState(0);
  useLayoutEffect(() => {
    if (!ref.current) return undefined;
    setWidth(ref.current.clientWidth);
    if (typeof ResizeObserver === 'undefined') return undefined;
    const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width));
    observer.observe(ref.current);
    return () => observer.disconnect();
  }, []);
  return [ref, width];
}

function niceTicks(min, max, count = 4) {
  const span = max - min || Math.abs(max) || 1;
  const raw = span / count;
  const magnitude = 10 ** Math.floor(Math.log10(raw));
  const step = [1, 2, 2.5, 5, 10].map((m) => m * magnitude).find((m) => m >= raw);
  const ticks = [];
  for (let value = Math.floor(min / step) * step; value <= max + step / 2; value += step) ticks.push(value);
  return ticks;
}

/**
 * Day-by-day projected balance. One series needs no legend (the section title names it); with a
 * scenario, both series get a legend and direct labels, and the scenario is also dashed so
 * identity never depends on colour alone. The zero line and negative band are always drawn.
 */
export default function ProjectionChart({ series, lowest, ariaLabel }) {
  const theme = useTheme();
  const { t, i18n } = useTranslation();
  const [ref, width] = useWidth();
  const [hover, setHover] = useState(null);
  // Keyboard moves are announced; pointer moves are not (one announcement per pixel would drown the page).
  const [fromKeyboard, setFromKeyboard] = useState(false);
  const days = series[0]?.days || [];

  const geometry = useMemo(() => {
    const values = series.flatMap((item) => item.days.map((day) => Number(day.balance)));
    const ticks = niceTicks(Math.min(0, ...values), Math.max(0, ...values));
    const yMin = ticks[0];
    const yMax = ticks[ticks.length - 1];
    const plotWidth = width - PAD.left - PAD.right;
    const plotHeight = HEIGHT - PAD.top - PAD.bottom;
    const x = (index) => PAD.left + (days.length <= 1 ? 0 : (index / (days.length - 1)) * plotWidth);
    const y = (value) => PAD.top + ((yMax - Number(value)) / (yMax - yMin || 1)) * plotHeight;
    return { ticks, x, y, plotWidth, plotHeight };
  }, [series, width, days.length]);

  if (!days.length) return null;
  // Not measured yet (or no layout, as in tests): keep the height so nothing below jumps.
  if (width <= PAD.left + PAD.right) return <Box ref={ref} sx={{ width: '100%', height: HEIGHT }} />;
  const { ticks, x, y, plotHeight } = geometry;
  const path = (items) =>
    items.map((day, index) => `${index ? 'L' : 'M'}${x(index).toFixed(1)},${y(day.balance).toFixed(1)}`).join('');
  const labelEvery = Math.max(1, Math.round(days.length / (width < 520 ? 4 : 7)));
  const lowestIndex = lowest ? days.findIndex((day) => day.date === lowest.date) : -1;

  const last = days.length - 1;
  const clamp = (index) => Math.min(last, Math.max(0, index));
  // Mouse and touch: a tap on a day shows it, as hovering does.
  const onPointer = (event) => {
    const box = event.currentTarget.getBoundingClientRect();
    const relative = ((event.clientX - box.left) / box.width) * width;
    setFromKeyboard(false);
    setHover(clamp(Math.round(((relative - PAD.left) / (width - PAD.left - PAD.right)) * last)));
  };
  // Keyboard: the chart is one tab stop; arrows walk the days, Home/End jump to the ends, Esc hides.
  const onKeyDown = (event) => {
    const current = hover ?? (lowestIndex >= 0 ? lowestIndex : 0);
    const next = {
      ArrowRight: current + 1,
      ArrowLeft: current - 1,
      ArrowUp: current + 1,
      ArrowDown: current - 1,
      PageUp: current + 7,
      PageDown: current - 7,
      Home: 0,
      End: last,
    }[event.key];
    if (event.key === 'Escape') {
      setHover(null);
      return;
    }
    if (next === undefined) return;
    event.preventDefault();
    setFromKeyboard(true);
    setHover(clamp(next));
  };
  const hoverText =
    hover !== null &&
    [
      dayLabel(days[hover].date, i18n.language, { weekday: 'short', day: '2-digit', month: 'short' }),
      ...series.map((item) =>
        [series.length > 1 ? item.label : null, money(item.days[hover].balance, i18n.language)]
          .filter(Boolean)
          .join(': ')
      ),
    ].join(', ');

  return (
    <Box
      ref={ref}
      sx={{
        position: 'relative',
        width: '100%',
        '& > svg': { borderRadius: 1 },
        '& > svg:focus-visible': { outline: '2px solid', outlineColor: 'primary.main', outlineOffset: 2 },
      }}
    >
      {series.length > 1 && (
        <Stack direction="row" spacing={2} sx={{ p: 0, m: 0, mb: 1 }} component="ul">
          {series.map((item) => (
            <Stack
              key={item.key}
              component="li"
              direction="row"
              spacing={1}
              alignItems="center"
              sx={{ listStyle: 'none' }}
            >
              <Box component="svg" width={24} height={8} aria-hidden>
                <line
                  x1="0"
                  y1="4"
                  x2="24"
                  y2="4"
                  stroke={item.color}
                  strokeWidth="2"
                  strokeDasharray={item.dashed ? '5 4' : undefined}
                />
              </Box>
              <Typography variant="caption" color="text.secondary">
                {item.label}
              </Typography>
            </Stack>
          ))}
        </Stack>
      )}
      <svg
        width={width}
        height={HEIGHT}
        role="img"
        aria-label={`${ariaLabel} ${t('future.chartKeys')}`}
        tabIndex={0}
        onKeyDown={onKeyDown}
        onFocus={() => {
          setFromKeyboard(true);
          setHover((previous) => previous ?? (lowestIndex >= 0 ? lowestIndex : 0));
        }}
        onBlur={() => setHover(null)}
        onPointerMove={onPointer}
        onPointerDown={onPointer}
        onPointerLeave={(event) => event.pointerType === 'mouse' && setHover(null)}
        style={{ display: 'block', touchAction: 'pan-y' }}
      >
        {/* negative band: everything under zero */}
        <rect
          x={PAD.left}
          y={y(0)}
          width={width - PAD.left - PAD.right}
          height={Math.max(0, PAD.top + plotHeight - y(0))}
          fill={theme.palette.chart.negative}
        />
        {ticks.map((tick) => (
          <g key={tick}>
            <line
              x1={PAD.left}
              x2={width - PAD.right}
              y1={y(tick)}
              y2={y(tick)}
              stroke={tick === 0 ? theme.palette.brand.lineStrong : theme.palette.chart.grid}
              strokeWidth={1}
            />
            <text x={PAD.left - 8} y={y(tick) + 4} textAnchor="end" fontSize="11" fill={theme.palette.text.secondary}>
              {tick === 0 ? t('future.chartZero') : compactMoney(tick, i18n.language)}
            </text>
          </g>
        ))}
        {days.map((day, index) =>
          index % labelEvery === 0 ? (
            <text
              key={day.date}
              x={x(index)}
              y={HEIGHT - 8}
              textAnchor="middle"
              fontSize="11"
              fill={theme.palette.text.secondary}
            >
              {dayLabel(day.date, i18n.language)}
            </text>
          ) : null
        )}
        {series.map((item) => (
          <path
            key={item.key}
            d={path(item.days)}
            fill="none"
            stroke={item.color}
            strokeWidth={2}
            strokeLinejoin="round"
            strokeLinecap="round"
            strokeDasharray={item.dashed ? '6 5' : undefined}
          />
        ))}
        {series.length > 1 &&
          series.map((item) => (
            <text
              key={`${item.key}-label`}
              x={width - PAD.right - 4}
              y={y(item.days[item.days.length - 1].balance) - 8}
              textAnchor="end"
              fontSize="11"
              fill={theme.palette.text.secondary}
            >
              {item.label}
            </text>
          ))}
        {lowestIndex >= 0 && (
          <g>
            <circle
              cx={x(lowestIndex)}
              cy={y(lowest.balance)}
              r={5}
              fill={Number(lowest.balance) < 0 ? theme.palette.error.main : theme.palette.text.primary}
              stroke={theme.palette.background.paper}
              strokeWidth={2}
            />
            {/* On the right half the label grows leftwards, so "-R$ 1.234,56" is never cut at the edge. */}
            <text
              x={x(lowestIndex) > width / 2 ? x(lowestIndex) - 8 : x(lowestIndex) + 8}
              textAnchor={x(lowestIndex) > width / 2 ? 'end' : 'start'}
              y={y(lowest.balance) + (y(lowest.balance) > PAD.top + plotHeight - 24 ? -10 : 18)}
              fontSize="11"
              fill={theme.palette.text.primary}
            >
              {t('future.chartLowest')}: {money(lowest.balance, i18n.language)}
            </text>
          </g>
        )}
        {hover !== null && (
          <g>
            <line
              x1={x(hover)}
              x2={x(hover)}
              y1={PAD.top}
              y2={PAD.top + plotHeight}
              stroke={theme.palette.text.secondary}
              strokeDasharray="2 3"
            />
            {series.map((item) => (
              <circle
                key={item.key}
                cx={x(hover)}
                cy={y(item.days[hover].balance)}
                r={4}
                fill={item.color}
                stroke={theme.palette.background.paper}
                strokeWidth={2}
              />
            ))}
          </g>
        )}
      </svg>
      <Box role="status" aria-live="polite" sx={visuallyHidden}>
        {fromKeyboard && hoverText}
      </Box>
      {hover !== null && (
        <Box
          aria-hidden="true"
          sx={{
            position: 'absolute',
            top: series.length > 1 ? 32 : 8,
            left: Math.min(Math.max(x(hover) + 12, 0), width - 200),
            pointerEvents: 'none',
            bgcolor: 'background.raised',
            border: '1px solid',
            borderColor: 'divider',
            borderRadius: 1.5,
            px: 1.5,
            py: 1,
            minWidth: 170,
          }}
        >
          <Typography variant="caption" color="text.secondary">
            {dayLabel(days[hover].date, i18n.language, { weekday: 'short', day: '2-digit', month: 'short' })}
          </Typography>
          {series.map((item) => (
            <Stack key={item.key} direction="row" justifyContent="space-between" spacing={2}>
              {series.length > 1 && (
                <Typography variant="caption" color="text.secondary">
                  {item.label}
                </Typography>
              )}
              <Typography variant="subtitle2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                {money(item.days[hover].balance, i18n.language)}
              </Typography>
            </Stack>
          ))}
        </Box>
      )}
    </Box>
  );
}

ProjectionChart.propTypes = {
  series: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      label: PropTypes.string,
      color: PropTypes.string.isRequired,
      dashed: PropTypes.bool,
      days: PropTypes.arrayOf(PropTypes.shape({ date: PropTypes.string, balance: PropTypes.number })).isRequired,
    })
  ).isRequired,
  lowest: PropTypes.shape({ date: PropTypes.string, balance: PropTypes.number }),
  ariaLabel: PropTypes.string.isRequired,
};
